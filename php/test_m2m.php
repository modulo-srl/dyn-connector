<?php

include_once('dyn-connector.php');


class test {

	private $access_token = null;

	/** Called when the connector needs last access token
	 *
	 * @return Last access token used
	 */
	public function get_access_token() {
		// Load from disk or database
		$this->access_token = @file_get_contents('dyn-connector.dat');

		echo "Connector requires access token (\"".$this->access_token."\")\n";

		return $this->access_token;
	}

	/** Called when the connector get a new access token
	 *
	 * @param string $access_token Token to save
	 */
	public function set_access_token($access_token) {
		$this->access_token = $access_token;

		echo "New access token \"".$this->access_token."\"\n";

		// Write to disk or database
		@file_put_contents('dyn-connector.dat', $this->access_token);
	}

	public function normal_request() {
		$domain = 'pigrecoos.it';

		$connector = new dyn_connector($domain);

		// Enable debugging output (should be disabled in production)
		$connector->set_debug(true);

		$data = array(
			'test' => array(
				'key1' => 1,
				'key2' => true,
				'key3' => 'This is a test'
			)
		);

		$result = $connector->send('test/echo', $data);

		echo "Server response: \n";
		print_r($result);
	}

	public function authenticated_request() {
		$domain = 'pigrecoos.it';
		$client_id = 'client ID';
		$client_secret = 'client secret';

		$connector = new dyn_connector($domain, $client_id, $client_secret,
			array($this, 'get_access_token'), array($this, 'set_access_token')
		);

		// Enable debugging output (should be disabled in production)
		$connector->set_debug(true);

		$data = array(
			'test' => array(
				'key1' => 1,
				'key2' => true,
				'key3' => 'This is a test'
			)
		);

		$result = $connector->send('auth/test/echo', $data);

		echo "Server response: \n";
		print_r($result);
	}

}



echo "<pre>";

$test = new test();

$test->normal_request();

echo "\n\n";

$test->authenticated_request();

echo "</pre>";
