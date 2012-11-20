basedir = ..
include ../Makefrag

CXX = g++
CXXFLAGS = -g -O2 -I./generated-src -I./testbench
LDFLAGS = -g

INCLUDES = generated-src/emulator.h generated-src/$(MODEL).h testbench/hsOptflow.h

default : all

partialDeriv-test: $(basedir)/src/partialDeriv.scala
	cd $(basedir)/sbt ; $(SBT) "project work" "run partialDerivTest --backend c --compile --targetDir ../emulator/generated-src --genHarness --test"

uvCalc-test: $(basedir)/src/uvCalc.scala
	cd $(basedir)/sbt ; $(SBT) "project work" "run uvCalcTest --backend c --compile --targetDir ../emulator/generated-src --genHarness --test"

uvAvg-test: $(basedir)/src/uvAvg.scala
	cd $(basedir)/sbt ; $(SBT) "project work" "run uvAvgTest --backend c --compile --targetDir ../emulator/generated-src --genHarness --test"

window-test: $(basedir)/src/window5x5.scala
	cd $(basedir)/sbt ; $(SBT) "project work" "run windowBufTest --backend c --compile --targetDir ../emulator/generated-src --genHarness --test"

generated-src/$(MODEL).cpp: $(basedir)/src/*.scala
	cd $(basedir)/sbt && $(SBT) "project work" "run filter ${PIPE_STAGES} --backend c --targetDir ../emulator/generated-src --debug --vcd --Wall"

$(MODEL).o: generated-src/$(MODEL).cpp $(INCLUDES)
	$(CXX) $(CXXFLAGS) -c $<

hsOptflow.o: testbench/hsOptflow.cpp testbench/hsOptflow.h
	$(CXX) $(CXXFLAGS) -c $<

$(MODEL)-em.o: testbench/$(MODEL)-em.cpp generated-src/$(MODEL).cpp
	$(CXX) $(CXXFLAGS) -c $<

$(MODEL)-em: $(MODEL)-em.o $(MODEL).o hsOptflow.o
	$(CXX) $(LDFLAGS) -o $@ $+

run : $(MODEL)-em
	./$(MODEL)-em

# sed line is a hack in case trace file generation is interrupted (e.g. by a segfault)
# just remove the last line
#	sed -i '$$d' trace.vcd

# no input argu parsing right now, will implement later
run-debug : $(MODEL)-em
	./$(MODEL)-em -t

run-trace : $(MODEL)-em
	-./$(MODEL)-em -v
	vcd2vpd -q trace.vcd trace.vpd
	rm -f trace.vcd

all : $(MODEL)-em

clean :
	rm -rf hsOptflow.o $(MODEL)-em.o $(MODEL).o $(MODEL)-em *.vcd *.vpd DVEfiles generated-src/*

