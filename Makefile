START_TEST_COM=java -cp wire-runtime-2.1.0.jar:okio-1.5.0.jar:sparrowtest.jar
PROTO_OUT_PATH=app/src/main/java/
WIRE_COMPILE_PATH=wire-compiler-2.1.0-jar-with-dependencies.jar
PROTO_PATH=app/src/main/proto/sparrow.proto

proto:
	java -jar $(WIRE_COMPILE_PATH) --java_out=$(PROTO_OUT_PATH) --proto_path=. $(PROTO_PATH)
	
start-client:
	 $(START_TEST_COM) edu.berkeley.cs194.TestClient localhost

start-server:
	$(START_TEST_COM) edu.berkeley.cs194.TestServer