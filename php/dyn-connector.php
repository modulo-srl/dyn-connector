<?php

/**
 * Simple class for sending POST-JSON requests to a Dyn server
 *
 * @version 1.11
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
	private $protocol_by_host;


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
	 * @param string|array|null $custom_http_header String(s) contains raw header(s) to pass to every HTTP call
	 * @param bool $protocol_by_host When True the host passed to send_request() will contain protocol (http:// or https://)
	 */
	public function set_debug($debug_mode = true, $custom_http_header = null, $protocol_by_host = false) {
		$this->debug = $debug_mode;
		$this->custom_http_header = $custom_http_header;
		$this->protocol_by_host = $protocol_by_host;
	}

	/** Send request
	 *
	 * @param string $operation Operation to perform
	 * @param array|null $data Array of data to send to server
	 * @return array Server (or internal) response
	 */
	public function send($operation, $data) {
		if ($this->debug)
			echo '[Send request "'.$operation.'"]'."\n";

		$response = $this->send_request($operation, $data);

		if (isset($response['error'])) {
			$code = $response['error']['code'];

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

		if (!empty($response_data['auth'])) {
			$this->session_token = $response_data['session_token'];

			if (is_callable($this->set_sessiontoken_callback))
				$this->session_token = call_user_func($this->set_sessiontoken_callback, $this->session_token);
			return true;
		}

		$this->session_token = null;
		return false;
	}

	/** Middle level POST request
	 *
	 * @param string $operation
	 * @param array|null $data
	 * @return array
	 */
	private function send_request($operation, $data) {
		if ($this->protocol_by_host)
			$url = '';
		else
			$url = 'https://';

		$url .= $this->host.'/api/'.$operation;

		if (!$data)
			$data = array();

		if ($operation !== 'auth') {
			if ($this->session_token)
				$data['session_token'] = $this->session_token;
		}

		$result = $this->get_content($url, $data, $this->custom_http_header, $error_message);

		if (!$result) {
			if (!$error_message)
				$error_message = 'unknown error';

			$result = array(
				'error' => array(
					'code' => -1,
					'reason' => $error_message
				)
			);
		}

		return $result;
	}

	/** Low level HTTP call
	 *
	 * Note: $custom_header could be an array of strings, or a string, without "\n\r" terminators.
	 *
	 * @param string $remote_url
	 * @param array|null $post_data
	 * @param string|array|null $custom_header Custom header to add (string or array of lines)
	 * @param string $error_message if set, store errors in this variable
	 * @param int|null $timeout_secs (optional, default 60 seconds by default)
	 * @return array|false
	 */
	private function get_content($remote_url, $post_data = null, $custom_header = null, &$error_message = null, $timeout_secs = null){

		$header = "Connection: close\r\n";

		if (empty($post_data)){
			$post_data = array();
		}

		$json_post = json_encode($post_data);

		//$header .= "Content-Type: application/x-www-form-urlencoded\r\n";
		//$header .= "Content-Type: multipart/form-data\r\n";

		$header .= "Content-Length: ".strlen($json_post)."\r\n";

		if ($custom_header){
			// add custom header
			if (is_array($custom_header)){
				foreach ($custom_header as $s)
					$header .= trim((string)$s)."\r\n";
			}else
				$header .= trim((string)$custom_header)."\r\n";
		}

		$context_arr = array(
			'http' => array(
				'method' => "POST",
				'header' => $header,
				'content' => $json_post
			)
		);

		if ($timeout_secs)
			$context_arr['http']['timeout'] = $timeout_secs;

		$context = stream_context_create($context_arr);

		$old_socket_timeout = ini_get('default_socket_timeout');
		if ($timeout_secs) {
			ini_set('default_socket_timeout', $timeout_secs);
			set_time_limit($timeout_secs * 2);
		} else {
			set_time_limit(60);  // 60 seconds timeout
		}

		$content = @file_get_contents($remote_url, false, $context);

		ini_set('default_socket_timeout', $old_socket_timeout);

		if ($content === false){
			$errors = error_get_last();
			if (empty($errors))
				$error_message = 'general connection error';
			else
				$error_message = $errors['type'].': '.$errors['message'];
		}

		if ($content) {
			$decoded = json_decode($content, true);
			if (!$decoded)
				$decoded = array(
					'error' => array(
						'code' => -1,
						'reason' => 'malformed response'
					)
				);
		}else{
			$decoded = array(
				'error' => array(
					'code' => -1,
					'reason' => 'empty response'
				)
			);

		}

		return $decoded;
	}

}