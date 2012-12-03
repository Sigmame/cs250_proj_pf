package Work {

import Chisel._
import Node._

import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class windowBuf3x3(imageWidth: Integer, doutWidth: Integer, memWidth : Integer) extends Component {
  val windowSize = 9
  val io = new Bundle {
    val din = UFix(INPUT, doutWidth)
    val dout = Vec(windowSize) { UFix(dir = OUTPUT, width = doutWidth) }
  }
  //instantiate SRAM 1024x32 (4*8bit)
  val row1buf = Mem(imageWidth,seqRead=true){UFix(width=memWidth)}
  val row2buf = Mem(imageWidth,seqRead=true){UFix(width=memWidth)}
  val readOut1 = Reg(resetVal = UFix(0,doutWidth))
  val readOut2 = Reg(resetVal = UFix(0,doutWidth))
  val out_reg = Vec(windowSize){Reg(resetVal = UFix(0,doutWidth))}
  val count = Reg(resetVal = UFix(0,log2Up(imageWidth)))

  io.dout := out_reg
  out_reg(0) := io.din
  //counter count from 0 to imageWidth-1-xwin
  when (count != UFix(imageWidth-4)){
    count := count + UFix(1)}
  when (count === UFix(imageWidth-4)){
    count := UFix(0)}
  //shift register
  for (i <- 1 until windowSize){
    if (i!=3 && i!=6){
      out_reg(i) := out_reg(i-1)}}
  out_reg(3) := readOut1(doutWidth-1,0)
  out_reg(6) := readOut2(doutWidth-1,0)
      //write memory and read memory
  val readAddr = Mux(count===UFix(imageWidth-4),UFix(0),count+UFix(1))
  row1buf(count) := out_reg(2)
  row2buf(count) := out_reg(5)
  readOut1 := row1buf(readAddr) 
  readOut2 := row2buf(readAddr) 
}
// Scala Unit Test for Window Buffer
class windowBuf3x3test(c: windowBuf3x3) extends Tester(c, Array(c.io)) { //binds the tester to windowBuf and test its io
  defTests {
    var allGood = true // test result boolean: true->pass
    val vars    = new HashMap[Node, Node]() // mapping of test nodes to literals
    val rnd     = new Random() // random number generator
    val maxInt  = 255 // 8-bit wide integer
    val xdim    = 128 
    val xwin    = 3
    val ywin    = 3
    val nrow    = 8
    val inputs  = new Array[Integer](xdim*nrow) // image input pixels
    
    for (i <- 0 until xdim*nrow) { 
      inputs(i) = rnd.nextInt(maxInt) // generate random input image with 8-bit per pixel
//      inputs(i) = i // regular numbers for easy-debug
    }
    val nd11 = xdim*(ywin-1)+xwin
    for (i <- 0 until nd11) { // delay cycles, no trace
      vars(c.io.din) = UFix(inputs(i))
      step(vars, isTrace = false)
    }
    for (k <- 0 until 5) {//xdim*nrow-nd11) { // start testing
      println(k)
    	vars(c.io.din) = UFix(inputs(k+nd11))
    	for (i <- 0 until xwin){
         for (j <- 0 until ywin){
           vars(c.io.dout(i*3+j)) = UFix(inputs(xdim*(2-i)+k+(2-j)));
         }
       }
    	allGood = step(vars) && allGood // steps the DUT and compare to expected values in vars
    }
    allGood // all TRUE -> pass
  }
}
}

