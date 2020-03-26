package connector

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"time"
)

// Connector object
type Connector struct {
	Debug bool

	host        string
	authUID     string
	masterToken string

	session      SessionPersistor
	sessionToken string

	customHTTPheader map[string]string
	protocolByHost   bool
}

// NewConnector make a new Dyn Connector
func NewConnector(host, authUID, masterToken string, session SessionPersistor) *Connector {
	conn := Connector{
		host:        host,
		authUID:     authUID,
		masterToken: masterToken,
	}

	conn.session = session
	conn.sessionToken = conn.session.GetSessionToken()

	return &conn
}

// SetDebug enable or disable debug and internal params
func (c *Connector) SetDebug(enableDebug bool, customHTTPheader map[string]string, protocolByHost bool) {
	c.Debug = enableDebug
	c.customHTTPheader = customHTTPheader
	c.protocolByHost = protocolByHost
}

// Send operation request, passing data and receiving custom data types
func (c *Connector) Send(operation string, dataSend, dataReceive interface{}) *ResponseError {
	if c.Debug {
		fmt.Println("[send request \"" + operation + "\"]")
	}

	err := c.sendRequest(operation, dataSend, dataReceive)
	if err != nil {
		switch err.Code {
		case 70: // Authentication needed, try to login
			if c.Debug {
				fmt.Println("[authentication needed, try to login...]")
			}

			err := c.doAuth()
			if err != nil {
				return err
			}

			// Retry request
			if c.Debug {
				fmt.Println("[authentication done, session token \"" + c.sessionToken + "\"]")
				fmt.Println("[resend request \"" + operation + "\"]")
			}

			err = c.sendRequest(operation, dataSend, dataReceive)
			if err != nil {
				return err
			}

		default:
			return err
		}
	}

	return nil
}

func (c *Connector) doAuth() *ResponseError {
	c.sessionToken = ""

	req := authRequest{
		UID:         c.authUID,
		MasterToken: c.masterToken,
	}

	resp := authResponse{}

	err := c.sendRequest("auth", req, &resp)
	if err != nil {
		return err
	}

	if !resp.Auth {
		return &ResponseError{
			Code:   -10,
			Reason: "invalid auth response",
		}
	}

	c.sessionToken = resp.SessionToken
	c.session.SetSessionToken(c.sessionToken)
	return nil
}

func (c *Connector) sendRequest(operation string, dataSend, dataReceive interface{}) *ResponseError {
	var err error

	url := ""
	if !c.protocolByHost {
		url = "https://"
	}
	url += c.host + "/api/" + operation

	if c.Debug {
		fmt.Println("[make POST to", url, "]")
	}

	client := http.Client{}
	client.Timeout = time.Duration(time.Second * 60) // 60 seconds timeout

	var requestBody []byte
	requestBody, err = json.Marshal(dataSend)
	if err != nil {
		return &ResponseError{
			Code:   -1,
			Reason: err.Error(),
		}
	}
	if c.Debug {
		fmt.Println("[raw request", string(requestBody), "]")
	}

	httpReq, err := http.NewRequest("POST", url, bytes.NewBuffer(requestBody))
	if err != nil {
		return &ResponseError{
			Code:   -1,
			Reason: err.Error(),
		}
	}

	httpReq.Header.Set("Content-type", "application/json")
	if len(c.customHTTPheader) > 0 {
		for key, value := range c.customHTTPheader {
			httpReq.Header.Set(key, value)
		}
	}
	if operation != "auth" && c.sessionToken != "" {
		httpReq.Header.Set("Session-Token", c.sessionToken)
	}

	httpResp, err := client.Do(httpReq)
	if err != nil {
		return &ResponseError{
			Code:   -1,
			Reason: err.Error(),
		}
	}
	defer httpResp.Body.Close()

	/*if c.Debug {
		body, _ := ioutil.ReadAll(httpResp.Body)
		fmt.Printf("[server raw response: `%s`\n", string(body))
	}*/

	_, ok := httpResp.Header["Error"]
	if ok {
		/*if c.Debug {
			fmt.Println("[decode error]")
		}*/

		rErr := responseErrorContainer{}
		err = json.NewDecoder(httpResp.Body).Decode(&rErr)
		if err != nil {
			return &ResponseError{
				Code:   -2,
				Reason: err.Error(),
			}
		}
		//fmt.Println(rErr)
		return &rErr.Error
	}

	//err := json.Unmarshal(body, &dataReceive)
	err = json.NewDecoder(httpResp.Body).Decode(&dataReceive)
	if err != nil {
		return &ResponseError{
			Code:   -2,
			Reason: err.Error(),
		}
	}

	return nil
}
