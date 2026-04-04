SOURCES := $(shell find src -name "*.java")
OUT := out

.PHONY: build clean discipline

build: $(SOURCES)
	@mkdir -p $(OUT)
	javac -d $(OUT) --release 21 $(SOURCES)
	@echo "build ok — $$(echo $(SOURCES) | wc -w) files"

clean:
	rm -rf $(OUT)

discipline:
	javac -d $(OUT) --release 21 discipline/Scanner.java
	java -cp $(OUT) discipline.Scanner src
