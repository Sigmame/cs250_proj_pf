package Work {

import Chisel._
import Node._

import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class windowBuf5x5(imageWidth: Integer, dataWidth: Integer) extends Component {
  val windowSize = 25
  val io = new Bundle {
    val din = UFix(INPUT, dataWidth)
    val dout = Vec(windowSize) { UFix(dir = OUTPUT, width = dataWidth) }
  }
  //instantiate SRAM 1024x32 (4*8bit)
  val rowbuf = Mem(imageWidth,seqRead=true){UFix(width=4*dataWidth)}
  val readOut = Reg(resetVal = UFix(0,4*dataWidth))
  val out_reg = Vec(windowSize){Reg(resetVal = UFix(0,dataWidth))}
  val count = Reg(resetVal = UFix(0,log2Up(imageWidth)))

  out_reg(0) := io.din
  io.dout := out_reg
  //counter
  when (count != UFix(imageWidth-6)){
    count := count + UFix(1)}
  when (count === UFix(imageWidth-6)){
    count := UFix(0)}
  //shift register
  for (i <- 1 until windowSize){
    if (i!=5 && i!=10 && i!=15 && i!=20){
      out_reg(i) := out_reg(i-1)}}
  out_reg(5)  := readOut(dataWidth-1,0)
  out_reg(10) := readOut(2*dataWidth-1,dataWidth)
  out_reg(15) := readOut(3*dataWidth-1,2*dataWidth)
  out_reg(20) := readOut(4*dataWidth-1,3*dataWidth)
      //write memory and read memory
  val readAddr = Mux(count===UFix(imageWidth-6),UFix(0),count+UFix(1))
  rowbuf(count) := Cat(out_reg(19),out_reg(14),out_reg(9),out_reg(4)).toUFix()
  readOut := rowbuf(readAddr) 

}
// Scala Unit Test for Window Buffer
class windowBuf5x5test(c: windowBuf5x5) extends Tester(c, Array(c.io)) { //binds the tester to windowBuf and test its io
  defTests {
    var allGood = true // test result boolean: true->pass
    val vars    = new HashMap[Node, Node]() // mapping of test nodes to literals
    val rnd     = new Random() // random number generator
    val maxInt  = 255 // 8-bit wide integer
    val xdim    = 128 
    val xwin    = 5
    val ywin    = 5
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
           println(k)
           println(i*5+j, i, j)
           println(xdim*(4-i)+(4-j))
           vars(c.io.dout(i*5+j)) = UFix(inputs(xdim*(4-i)+k+(4-j)));
         }
       }
    	allGood = step(vars) && allGood // steps the DUT and compare to expected values in vars
    }
    allGood // all TRUE -> pass
  }
}
}

