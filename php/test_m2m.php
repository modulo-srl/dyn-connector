<?php

include_once('dyn-connector.php');


class test {

	private $session_token = null;

	/** Called when the connector needs last session token
	 *
	 * @return Last session token used
	 */
	public function get_session_token() {
		// Load from disk or database
		$this->session_token = @file_get_contents('dyn-connector.dat');

		echo "Connector requires session token (\"".$this->session_token."\")\n";

		return $this->session_token;
	}

	/** Called when the connector get a new session token
	 *
	 * @param string $session_token Token to save
	 */
	public function set_session_token($session_token) {
		$this->session_token = $session_token;

		echo "New session token \"".$this->session_token."\"\n";

		// Write to disk or database
		@file_put_contents('dyn-connector.dat', $this->session_token);
	}

	public function main() {
		$domain = 'pigrecoos.it';
		$auth_uid = 'test';
		$master_token = 'test';

		$connector = new dyn_connector($domain, $auth_uid, $master_token,
			array($this, 'get_session_token'), array($this, 'set_session_token')
		);

		// Enable debugging output (please disable in production)
		$connector->set_debug(true);

		$data = array(
			'test' => array(
				'key1' => 1,
				'key2' => true,
				'key3' => 'This is a test'
			)
		);

		$result = $connector->send('echo', $data);

		echo "Server response: \n";
		print_r($result);
	}

}



echo "<pre>";
$test = new test();
$test->main();
echo "</pre>";