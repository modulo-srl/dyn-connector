<?php

/**
 * Simple class for sending POST-JSON requests to a Dyn server
 *
 * @version 1.3
 * @author Modulo srl
 */
class dyn_connector {

	private $host;
	private $auth_UID;
	private $master_token;

	private $get_sessiontoken_callback;
	private $set_sessiontoken_callback;

	private $session_token;
	private $debug;
	private $custom_http_header;


	/** dyn_connector constructor.
	 *
	 * @param string $host Domain without protocol, ex: "www.domain.com"
	 * @param string $auth_UID Auth UID
	 * @param string $master_token Master token
	 * @param array|string|null $get_sessiontoken_callback Function name or array(class, method_name)
	 * @param array|string|null $set_sessiontoken_callback Function name or array(class, method_name)
	 */
	public function __construct($host, $auth_UID, $master_token,
										 $get_sessiontoken_callback, $set_sessiontoken_callback) {
		if (substr($host, 0, 4) != 'http')
			$host = 'https://'.$host;
		$this->host = $host;

		$this->auth_UID = $auth_UID;
		$this->master_token = $master_token;

		$this->get_sessiontoken_callback = $get_sessiontoken_callback;
		$this->set_sessiontoken_callback = $set_sessiontoken_callback;

		if (is_callable($this->get_sessiontoken_callback))
			$this->session_token = call_user_func($this->get_sessiontoken_callback);
	}

	/** Enable or disable debugging output
	 *
	 * @param bool $debug_mode Enable or disable debugging
	 * @param array|null $custom_http_header String(s) contains a custom header to pass to every HTTP call
	 */
	public function set_debug($debug_mode = true, $custom_http_header = null) {
		$this->debug = $debug_mode;
		$this->custom_http_header = $custom_http_header;
	}

	/** Send request
	 *
	 * @param string $operation Operation to perform
	 * @param mixed|null $data Data to send to server
	 * @return mixed|false Server (or internal) response
	 */
	public function send($operation, $data) {
		if ($this->debug)
			echo '[Send request "'.$operation.'"]'."\n";

		$response = $this->send_request($operation, $data);

		if (isset($response->error)) {
			$code = $response->error->code;

			if ($code == 70) {
				// Authentication needed, try to login

				if ($this->debug)
					echo "[Authentication needed, try to login...]\n";

				if ($this->do_auth($response)) {
					// Retry request

					if ($this->debug)
						echo "[Authentication done, session token \"".$this->session_token."\"]\n";

					if ($this->debug)
						echo '[Resend request "'.$operation.'"]'."\n";
					$response = $this->send_request($operation, $data);
				}
			}
		}

		if ($this->debug)
			echo "\n";

		return $response;
	}

	/** Perform Auth
	 *
	 * @param array $response_data
	 * @return bool
	 */
	private function do_auth(&$response_data = null) {
		$data = array(
			'uid' => $this->auth_UID,
			'master_token' => $this->master_token
		);

		$response_data = $this->send_request('auth', $data);

		if (!empty($response_data->auth)) {
			$this->session_token = $response_data->session_token;

			if (is_callable($this->set_sessiontoken_callback))
				call_user_func($this->set_sessiontoken_callback, $this->session_token);
			return true;
		}

		$this->session_token = null;
		return false;
	}

	/** Middle level POST request
	 *
	 * @param string $operation
	 * @param array|null $data
	 * @return object
	 */
	private function send_request($operation, $data) {
		$url = $this->host.'/api/'.$operation;

		$headers = $this->custom_http_header;

		if ($operation !== 'auth') {
			if ($this->session_token) {
				//$data['session_token'] = $this->session_token;

				if (!$headers)
					$headers = [];
				$headers[] = 'Session-Token: '.$this->session_token;
			}
		}

		$result = $this->get_content($url, $data, $headers, $error_message);

		if (!$result) {
			if (!$error_message)
				$error_message = 'unknown error';

			$result = (object)[
				'error' => (object)[
					'code' => -1,
					'reason' => $error_message
				]
			];
		}

		return $result;
	}

	/** Low level HTTP call
	 *
	 * Note: $custom_header could be an array of strings, or a string, without "\n\r" terminators.
	 *
	 * @param string $remote_url
	 * @param array|null $post_data
	 * @param array|null $custom_header Custom header to add (array of strings)
	 * @param string $error_message if set, store errors in this variable
	 * @param int|null $timeout_secs (optional, default 60 seconds by default)
	 * @return object|false
	 */
	private function get_content($remote_url, $post_data = null, $custom_header = null, &$error_message = null, $timeout_secs = null){
		$header = '';
		//$header = "Connection: close\r\n";
		$header .= "Content-Type: application/json\r\n";

		if (empty($post_data)){
			$post_data = array();
		}

		$json_post = json_encode($post_data);

		if ($custom_header){
			// add custom header
			foreach ($custom_header as $s) {
				$s = trim((string)$s);
				if ($s)
					$header .= $s."\r\n";
			}
		}

		$context_arr = array(
			'http' => array(
				'method' => "POST",
				'protocol_version' => 1.1,
				'ignore_errors' => true,
				'header' => $header,
				'content' => $json_post
			)
		);

		// Set timeout
		if ($timeout_secs) {
			$context_arr['http']['timeout'] = $timeout_secs;

			$old_socket_timeout = ini_get('default_socket_timeout');
			ini_set('default_socket_timeout', $timeout_secs);
		}
		set_time_limit(60*30);  // 30 minutes timeout

		// Make the call
		$error_message = null;
		$content = null;
		try {
			$context = stream_context_create($context_arr);
			$content = @file_get_contents($remote_url, false, $context);
		} catch (\Exception $e) {
			$error_message = $e->getMessage();
		}

		if ((($content === "") || ($content === false)) && !$error_message) {
			$errors = error_get_last();
			if ($errors) {
				$error_message = $errors['type'].': '.$errors['message'];
			} else {
				if (!empty($http_response_header)) {
					$code = self::get_http_return_code($http_response_header, $text);
					if ($code >= 400)
						$error_message = $code.' '.$text;
				}

				if (!$error_message && ($content === false))  // unknown error internal to file_get_contents()
					$error_message = 'general connection error';
			}
		}

		// Restore timeouts
		if ($timeout_secs)
			ini_set('default_socket_timeout', $old_socket_timeout);

		if ($error_message || !strlen($content))
			return false;

		$out = json_decode($content);
		if ($out === null) {
			if ($content === '')
				$error_message = 'empty response';
			else
				$error_message = 'malformed response';

			return false;
		}

		return $out;
	}

	static private function get_http_return_code($http_response_header, &$text = null) {
		$text = null;

		if (is_array($http_response_header) && count($http_response_header)) {
			$parts = explode(' ',$http_response_header[0]);

			if (count($parts)) { //HTTP/1.X <code> <text>
				$text = implode(' ', array_slice($parts, 2));
				return intval($parts[1]); // Get code
			}
		}

		return 0;
	}


}
