package main

import (
	"fmt"

	"modulo.srl/DynConnector/connector"
)

// Session persistor
type session struct {
	sessionToken string
}

func (s *session) GetSessionToken() string {
	fmt.Println("Connector requires session token (\"" + s.sessionToken + "\")")
	return s.sessionToken
}

func (s *session) SetSessionToken(token string) {
	s.sessionToken = token
	fmt.Println("New session token \"" + s.sessionToken + "\"")
}

// Test data to send and receive
type testData struct {
	KeyA    int64
	SubData testSubData
}
type testSubData struct {
	Key1 int
	Key2 bool
	Key3 string
}

func main() {
	domain := "modulo.srl"

	authUID := "test"
	masterToken := "test"

	session := session{}
	connector := connector.NewConnector(domain, authUID, masterToken, &session)

	// Enable debugging output (please disable in production)
	connector.SetDebug(true, nil, false)

	dataSend := testData{
		KeyA: 1024,
		SubData: testSubData{
			Key1: 1,
			Key2: true,
			Key3: "This is a test",
		},
	}
	dataReceive := testData{}

	/*dataSend := map[string]interface{}{
		"KeyA": 1024,
		"SubData": map[string]interface{}{
			"Key1": 1,
			"Key2": true,
			"Key3": "This is a test",
		},
	}
	dataReceive := map[string]interface{}{}
	*/

	err := connector.Send("echo", dataSend, &dataReceive)
	//err := connector.Send("echo/auth", dataSend, &dataReceive)

	if err != nil {
		fmt.Println("Error:", err.Error())
		return
	}

	fmt.Println("Server response:", dataReceive)
}
