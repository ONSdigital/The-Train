package main

import (
	"fmt"
	"io/fs"
	"io/ioutil"
	"os"
	"strings"
	"testing"

	"github.com/ONSdigital/dp-the-train-feature-tests/config"

	. "github.com/smartystreets/goconvey/convey"
)

func removeElement(s []fs.FileInfo, i int) []fs.FileInfo {
	s[i] = s[len(s) - 1]
	return s[:len(s) - 1]
}

func TestMain(m *testing.M) {
	// Delete any transactions from previous tests.
	con, err := config.Get()
	CheckErrorWithMsg(err, "failed getting env variable for transaction store")
	transacStore = con.TransactionStore
	// Remove all files and folders within the Transaction Store in preparation.
	os.RemoveAll(transacStore)
	// Execute the test
	exitCode := m.Run()
	os.Exit(exitCode)
}

func TestItemsArchivedCorrectly(t *testing.T) {
	// Get the TransactionStore path
	con, err := config.Get()
	CheckErrorWithMsg(err, "failed getting env variable for transaction store")
	transacStore = con.TransactionStore

	// Check only Transactions beginning with futureOK are left in the folder after startup
	files, err := ioutil.ReadDir(transacStore)
	CheckErrorWithMsg(err, "failed: reading files in transaction store")
	if err != nil {
		fmt.Println(err)
	}

	// Remove any hidden files or folder from directory listing
	for i, fileInfo := range (files) {
		if strings.HasPrefix(fileInfo.Name(), ".") {
			files = removeElement(files, i)
		}
	}

	Convey("After The-Train startup", t, func() {
		Convey("Check files were appropriately archived", func() {
			So(len(files), ShouldEqual, 2)
			So(files[0].Name(), ShouldStartWith, "futureOK")
			So(files[1].Name(), ShouldStartWith, "futureOK")
		})
	})
}