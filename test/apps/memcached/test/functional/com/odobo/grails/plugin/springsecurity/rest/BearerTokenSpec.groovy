package com.odobo.grails.plugin.springsecurity.rest

import grails.plugins.rest.client.ErrorResponse
import grails.plugins.rest.client.RestResponse
import grails.util.Holders
import spock.lang.IgnoreIf
import spock.lang.IgnoreRest
import spock.lang.Issue

@IgnoreIf({ !Holders.config.grails.plugin.springsecurity.rest.token.validation.useBearerToken })
@Issue("https://github.com/alvarosanchez/grails-spring-security-rest/issues/73")
class BearerTokenSpec extends AbstractRestSpec {

    void "access token response is compliant with the specification"() {
        when:
        RestResponse response = sendCorrectCredentials()

        then:
        response.status == 200
        response.headers.getFirst('Content-Type') == 'application/json;charset=UTF-8'
        response.headers.getFirst('Cache-Control') == 'no-store'
        response.headers.getFirst('Pragma') == 'no-cache'
        response.json.access_token
        response.json.token_type == 'Bearer'

    }

    void "authorisation header is checked to read token value"() {
        given:
        RestResponse authResponse = sendCorrectCredentials()
        String token = authResponse.json.access_token

        when:
        def response = restBuilder.get("${baseUrl}/secured") {
            header 'Authorization', "Bearer ${token}"
        }

        then:
        response.status == 200
    }

    void "Form-Encoded body parameter can be used"() {
        given:
        RestResponse authResponse = sendCorrectCredentials()
        String token = authResponse.json.access_token

        when:
        def response = restBuilder.post("${baseUrl}/secured") {
            contentType 'application/x-www-form-urlencoded'
            body "access_token=${token}".toString()
        }

        then:
        response.status == 200
    }

    void "query string can be used"() {
        given:
        RestResponse authResponse = sendCorrectCredentials()
        String token = authResponse.json.access_token

        when:
        def response = restBuilder.get("${baseUrl}/secured?access_token=${token}")

        then:
        response.status == 200

    }

    void "if credentials are required but missing, the response contains WWW-Authenticate header"() {
        when:
        ErrorResponse response = restBuilder.post("${baseUrl}/secured") {
            contentType 'application/x-www-form-urlencoded'
        }

        then:
        response.status == 401
        response.responseHeaders.getFirst('WWW-Authenticate') == 'Bearer'
    }

    void "if the token is invalid, it is indicated in the header"() {
        when:
        ErrorResponse response = restBuilder.get("${baseUrl}/secured") {
            header 'Authorization', "Bearer wrongTokenValue"
        }

        then:
        response.status == 401
        response.responseHeaders.getFirst('WWW-Authenticate') == 'Bearer error="invalid_token"'
    }

    void "when accessing a secured object with a non-bearer request, it's considered a unauthorized request"() {
        when:
        ErrorResponse response = restBuilder.post("${baseUrl}/secured") {
            contentType 'text/plain'
            body "{hi:777}"
        }

        then:
        response.status == 401
        response.responseHeaders.getFirst('WWW-Authenticate') == 'Bearer'
    }

    @Issue("https://github.com/alvarosanchez/grails-spring-security-rest/issues/81")
    void "authorisation header is checked to read token value for logout"() {
        given:
        RestResponse authResponse = sendCorrectCredentials()
        String token = authResponse.json.access_token

        when:
        def response = restBuilder.post("${baseUrl}/api/logout") {
            header 'Authorization', "Bearer ${token}"
        }

        then:
        response.status == 200
    }

    @Issue("https://github.com/alvarosanchez/grails-spring-security-rest/issues/81")
    void "Form-Encoded body parameter can be used for logout"() {
        given:
        RestResponse authResponse = sendCorrectCredentials()
        String token = authResponse.json.access_token

        when:
        def response = restBuilder.post("${baseUrl}/api/logout") {
            contentType 'application/x-www-form-urlencoded'
            body "access_token=${token}".toString()
        }

        then:
        response.status == 200
    }

    void "query string can't be used for logout as GET is not supported"() {
        given:
        RestResponse authResponse = sendCorrectCredentials()
        String token = authResponse.json.access_token

        when:
        def response = restBuilder.get("${baseUrl}/api/logout?access_token=${token}")

        then:
        response.status == 405
    }

    @Issue("https://github.com/alvarosanchez/grails-spring-security-rest/issues/98")
    void "accessing Anonymous without a token, responds  ok"() {
        when:
        def response = restBuilder.get("${baseUrl}/anonymous") {
            contentType 'application/json;charset=UTF-8'
        }

        then:
        response.status == 200
    }

    @Issue("https://github.com/alvarosanchez/grails-spring-security-rest/issues/98")
    void "accessing Secured without a token, responds Unauthorized"() {
        when:
        ErrorResponse response = restBuilder.post("${baseUrl}/secured") {
            contentType 'application/json;charset=UTF-8'
            body "{hi:777}"
        }

        then:
        response.status == 401
        response.responseHeaders.getFirst('WWW-Authenticate') == 'Bearer'
    }

    void "accessing Secured with valid token, but not authorized responds forbidden"() {
        given:
        RestResponse authResponse = sendCorrectCredentials()
        String token = authResponse.json.access_token

        when:
        def response = restBuilder.get("${baseUrl}/secured/superAdmin") {
            header 'Authorization', "Bearer ${token}"
        }

        then:
        response.status == 403
        response.responseHeaders.getFirst('WWW-Authenticate') == 'Bearer error="insufficient_scope"'
    }

}