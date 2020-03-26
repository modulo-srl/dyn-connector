package connector

import "fmt"

// SessionPersistor mantains the token persistent
type SessionPersistor interface {
	GetSessionToken() string
	SetSessionToken(string)
}

// ResponseError with error codes
type ResponseError struct {
	Code   int    `json:"code"`
	Reason string `json:"reason"`
}

func (err *ResponseError) Error() string {
	return fmt.Sprintf("[%v] %v", err.Code, err.Reason)
}

// ResponseError.Code
const (
	ErrorHTTP         = -1
	ErrorDecode       = -2
	ErrorAuthInternal = -10
)

type responseErrorContainer struct {
	Error ResponseError `json:"error"`
}

type authRequest struct {
	UID         string `json:"uid"`
	MasterToken string `json:"master_token"`
}

type authResponse struct {
	Auth         bool   `json:"auth"`
	SessionToken string `json:"session_token"`
}
