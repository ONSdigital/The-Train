package main

import (
	"bytes"
	"context"
	"fmt"
	"io"
	"io/ioutil"
	"mime/multipart"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
	"testing"

	"github.com/ONSdigital/The-Train/src/test/go/config"
	"github.com/ONSdigital/log.go/v2/log"
	. "github.com/smartystreets/goconvey/convey"
)

func extractId(s string) string {
	re := regexp.MustCompile("\\\"(\\S){64}\\\"") // e.g. 16de96ef93ca546a82f5310c1320f2bf4957d3990361fc5d0a15734f998b80c5
	res := re.FindAllStringSubmatch(s, 1)
	for _ = range res {
		//like Java: match.group(1)
		if len(res)<1 {
			return ""
		}

		if len(res)>0 {
			id := res[0][0]
			id = id [ 1 : ] // Remove first " character
			id = id[:len(id)-1] //  Remove last " character
			return id
		}
	}
	return ""
}

func TestExtractId(t *testing.T) {
	Convey("When looking for an alphanumeric id of len 64", t, func() {
		Convey("The following should be found successfully in the following strings", func() {
			So(len(extractId("\"b4e4d74fceaff466a8a0aa64f98a7e77877f4f977c8ebb1155e514272e1a50a7\"")), ShouldEqual, 64)
			So(len(extractId("\"startDateObject\": \"Jan 31, 2022 1:05:10 PM\",\"id\": \"0076964b6b996cfc8031e421bd0c4091ac35b86ae67b5c468bea63ec16b850a9\",\"status\": \"started\",")), ShouldEqual, 64)
		})
	})
}

func getTransactionFolder(id string) string {
	// Get the TransactionStore path
	con, err := config.Get()
	ctx := context.Background()
	if err != nil {
		log.Fatal(ctx, "failed getting env variable for transaction store", err)
	}
	return con.TransactionStore
}

func getTransactionJSONPath(id string) string {
	// Copy the historic transactions to the Transaction Store
	return filepath.Join(getTransactionFolder(id), id, "transaction.json")
}

func readTransactionJSONFile(id string) string {
	path := getTransactionJSONPath(id)
	content, err := ioutil.ReadFile(path)
	ctx := context.Background()
	if err != nil {
		log.Fatal(ctx, "Failed: reading file", err)
	}
	contentString := byteArrToString(content)
	if len(content) > 0 {
		return contentString
	} else {
		return ""
	}
}

func byteArrToString(b []byte) string {
	return string(b[:])
}

// postBegin with the post body, returns the Transaction ID
func postBegin() (string, *http.Response, error) {
	postBody := bytes.NewBuffer(nil)
	resp, err := http.Post("http://localhost:8084/begin", "application/json", postBody)
	if err != nil {
		return "", nil, err
	}

	// Extract response
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return "", nil, err
	}

	return byteArrToString(body), resp, nil
}


// postCommitManifest with the post body, returns the Transaction ID
func postCommitManifest(transacID string) (string, *http.Response, error) {
	var postBodyStr = `{"filesToCopy":[],"urisToDelete":[]}`
	postBodyByte := bytes.NewBuffer([]byte(postBodyStr))
	resp, err := http.Post("http://localhost:8084/CommitManifest?transactionId=" + transacID, "application/json", postBodyByte)
	if err != nil {
		return "", nil, err
	}

	// Extract response
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return "", nil, err
	}

	return byteArrToString(body), resp, nil
}


func SendFilePostRequest (transacID string) (string, error) {
	url :=  fmt.Sprintf("http://localhost:8084/publish?transactionId=%s&uri=gabba", transacID)
	file, err := os.Open("README.md")
	if err != nil {
		return "", fmt.Errorf("failed: creating file object. %w", err)
	}
	defer file.Close()

	body := &bytes.Buffer{}
	writer := multipart.NewWriter(body)
	part, err := writer.CreateFormFile("file", filepath.Base(file.Name()))
	if err != nil {
		return "", fmt.Errorf("failed: creating form from file. %w", err)
	}

	io.Copy(part, file)
	writer.Close()
	request, err := http.NewRequest("POST", url, body)
	if err != nil {
		return "", fmt.Errorf("failed: creating http request %w", err)
	}

	request.Header.Add("Content-Type", writer.FormDataContentType())
	request.Header.Add("part", "null")
	request.Header.Add("multiParts", "null")
	request.Header.Add("uri", "test")
	client := &http.Client{}

	writer.Close()
	response, err := client.Do(request)
	if err != nil {
		return "", fmt.Errorf("failed: http request %w", err)
	}
	defer response.Body.Close()
	content, err := ioutil.ReadAll(response.Body)
	if err != nil {
		return "", fmt.Errorf("failed: reading response body %w", err)
	}

	return byteArrToString(content), nil
}

func checkKeyValuePairExists(key string, value string, body string) bool {
	// Example matches:
	// 	\"message\":\s\"New transaction created.\"
	//  \"key\":\s\"value.\"

	regExp := key + "(.){1,4}" + value
	//regExp := "\\\"" + key + "\\\":(.){1,4}" + value

	match, _ := regexp.MatchString(regExp, body)
	return match
}

func checkKeyValuePairExistsInFile(key string, value string, file string) bool {
	content, err := ioutil.ReadFile(file)
	ctx := context.Background()
	if err != nil {
		log.Fatal(ctx, "Failed: reading file", err)
	}

	return checkKeyValuePairExists(key, value, byteArrToString(content))
}


// TestCommitManifestEndpoints checks the commit endpoint
func TestBeginEndpoint(t *testing.T) {
	ctx := context.Background()
	body, response, err := postBegin()
	if err != nil {
		log.Fatal(ctx, "failed post /begin", err)
	}
	transacId := extractId(body)

	Convey("When calling /publish", t, func() {
		Convey("The Transaction should look like", func() {
			So(response.Status, ShouldEqual, "200 OK")
			So(checkKeyValuePairExistsInFile("status", "started", getTransactionJSONPath(transacId)), ShouldBeTrue)
			So(checkKeyValuePairExists("errors", "\\[\\]", body), ShouldBeTrue)
			So(checkKeyValuePairExists("uriDeletes", "\\[\\]", body), ShouldBeTrue)
			So(checkKeyValuePairExists("uriInfos", "\\[\\]", body), ShouldBeTrue)
			So(len(transacId), ShouldEqual, 64)
		})
	})
}

// TestCommitManifestEndpoints checks the publish endpoint
func TestPublishEndpoint(t *testing.T) {
	ctx := context.Background()
	var transacId string
	body, _, err := postBegin()
	if err != nil {
		log.Fatal(ctx, "failed post /begin", err)
	}
	transacId = extractId(body)
	var response *http.Response
	body, response, err = postCommitManifest(transacId)
	if err != nil {
		log.Fatal(ctx, "failed post /CommitManifest", err)
	}

	Convey("When calling /publish", t, func() {
		Convey("The Transaction should look like", func() {
			So(response.Status, ShouldEqual, "200 OK")
			So(checkKeyValuePairExists("errors", "\\[\\]", body), ShouldBeTrue)
			So(checkKeyValuePairExists("uriDeletes", "\\[\\]", body), ShouldBeTrue)
			So(checkKeyValuePairExists("uriInfos", "\\[\\]", body), ShouldBeTrue)
			So(checkKeyValuePairExists("status", "publishing", body), ShouldBeTrue)
			So(checkKeyValuePairExistsInFile("status", "publishing", getTransactionJSONPath(transacId)), ShouldBeTrue)
			So(len(transacId), ShouldEqual, 64)
		})
	})
}

// TestCommitManifestEndpoints checks the commit endpoint
func TestCommitManifestEndpoint(t *testing.T) {
	ctx := context.Background()
	body, _, err := postBegin()
	if err != nil {
		log.Fatal(ctx, "failed post /begin", err)
	}
	transacId := extractId(body)
	var response *http.Response
	body, response, err = postCommitManifest(transacId)
	if err != nil {
		log.Fatal(ctx, "failed post /CommitManifest", err)
	}
	body, err = SendFilePostRequest(transacId)
	if err != nil {
		log.Fatal(ctx, "failed doing multipart post on /publish", err)
	}
	Convey("When calling /publish", t, func() {
		Convey("The Transaction should look like", func() {
			So(response.Status, ShouldEqual, "200 OK")
			So(checkKeyValuePairExists("errors", "\\[\\]", body), ShouldBeTrue)
			So(checkKeyValuePairExists("uriDeletes", "\\[\\]", body), ShouldBeTrue)
			So(checkKeyValuePairExists("status", "publishing", body), ShouldBeTrue)
			So(checkKeyValuePairExists("uriInfos", "[[\\n\\r\\s]+{", body), ShouldBeTrue) // [\n\r\s]+ matches new line, cr, or space, which may appear between the [ and the {
			So(checkKeyValuePairExists("uri", "gabba", body), ShouldBeTrue)
			So(checkKeyValuePairExists("status", "uploaded", body), ShouldBeTrue)
			So(checkKeyValuePairExists("action", "created", body), ShouldBeTrue)
			So(checkKeyValuePairExistsInFile("status", "publishing", getTransactionJSONPath(transacId)), ShouldBeTrue)
			So(len(transacId), ShouldEqual, 64)
		})
	})
}

// TestCommitManifestEndpoints checks the commit endpoint
func TestDeletedTransaction(t *testing.T) {
	ctx := context.Background()
	body, _, err := postBegin()
	if err != nil {
		log.Fatal(ctx, "failed post /begin", err)
	}
	transacId := extractId(body)

	// Delete transaction.json file to trigger a rollback
	path := getTransactionJSONPath(transacId)
	//err = os.Remove(path)
	err = os.RemoveAll(filepath.Dir(path))

	var response *http.Response
	body, response, err = postCommitManifest(transacId)
	if err != nil {
		log.Fatal(ctx, "failed post /CommitManifest", err)
	}
	if err != nil {
		log.Fatal(ctx, "failed to delete transaction.json", err)
	}
	body, err = SendFilePostRequest(transacId)
	if err != nil {
		log.Fatal(ctx, "failed doing multipart post on /publish", err)
	}
	Convey("When calling /publish", t, func() {
		Convey("The Transaction should look like", func() {
			So(len(transacId), ShouldEqual, 64)
			So(response.Status, ShouldEqual, "500 Server Error")
			So(checkKeyValuePairExists("status", "publishing", body), ShouldBeTrue)
			So(checkKeyValuePairExists("error", "true", body), ShouldBeTrue)
			So(checkKeyValuePairExists("message", "error adding file to transaction", body), ShouldBeTrue)
		})
	})
}

// TestCommitManifestEndpoints checks the commit endpoint
func TestCommitManifestEndoint(t *testing.T) {
	ctx := context.Background()
	body, _, err := postBegin()
	if err != nil {
		log.Fatal(ctx, "failed post /begin", err)
	}
	transacId := extractId(body)

	var response *http.Response
	body, response, err = postCommitManifest(transacId)
	if err != nil {
		log.Fatal(ctx, "failed post /CommitManifest", err)
	}
	if err != nil {
		log.Fatal(ctx, "failed to delete transaction.json", err)
	}
	body, err = SendFilePostRequest(transacId)
	if err != nil {
		log.Fatal(ctx, "failed doing multipart post on /publish", err)
	}
	Convey("When calling /publish", t, func() {
		Convey("The Transaction should look like", func() {
			So(len(transacId), ShouldEqual, 64)
			transactionJsonFile := readTransactionJSONFile(transacId)
			So(response.Status, ShouldEqual, "200 OK")
			So(checkKeyValuePairExists("status", "publishing", body), ShouldBeTrue)
			So(checkKeyValuePairExists("status", "publishing", transactionJsonFile), ShouldBeTrue)

			So(checkKeyValuePairExists("error", "false", body), ShouldBeTrue)
			So(checkKeyValuePairExists("error", "[[]", transactionJsonFile), ShouldBeTrue)

			So(checkKeyValuePairExists("message", "Published to gabba", body), ShouldBeTrue)
			So(checkKeyValuePairExists("uri", "gabba", transactionJsonFile), ShouldBeTrue)
		})
	})
}