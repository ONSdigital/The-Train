package main

import (
	"context"
	"fmt"
	"io/fs"
	"io/ioutil"
	"os"
	"strings"
	"testing"

	"github.com/ONSdigital/The-Train/src/test/go/config"
	"github.com/ONSdigital/log.go/v2/log"
	. "github.com/smartystreets/goconvey/convey"
)

func removeElement(s []fs.FileInfo, i int) []fs.FileInfo {
	s[i] = s[len(s) - 1]
	return s[:len(s) - 1]
}

func TestMain(m *testing.M) {
	ctx := context.Background()
	// Delete any transactions from previous tests.
	con, err := config.Get()
	if err != nil {
		log.Fatal(ctx, "failed getting env variable for transaction store", err)
	}
	transacStore = con.TransactionStore
	// Remove all files and folders within the Transaction Store in preparation.


	os.RemoveAll(transacStore)
	if err := CreateIfNotExists(transacStore, 0755); err != nil {
		log.Fatal(ctx, "failed: recreate transaction folder", err)
		os.Exit(5)
	}

	// Execute the test
	exitCode := m.Run()
	os.Exit(exitCode)
}

func TestItemsArchivedCorrectly(t *testing.T) {
	ctx := context.Background()
	// Get the TransactionStore path
	con, err := config.Get()
	if err != nil {
		log.Fatal(ctx, "failed getting env variable for transaction store", err)
	}
	transacStore = con.TransactionStore

	// Check only Transactions beginning with futureOK are left in the folder after startup
	files, err := ioutil.ReadDir(transacStore)
	if err != nil {
		log.Fatal(ctx, "failed: reading files in transaction store", err)
	}
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