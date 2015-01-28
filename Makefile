PHONY: all test build clean deploy

clean:
	rm -rf target

build: test
	lein uberjar

test:
	lein test

all: build
