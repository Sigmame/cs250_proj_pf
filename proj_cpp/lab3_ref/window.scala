package Work {

import Chisel._
import Node._

// for testbench
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.util.Random


class rowBuf(xwin: Integer, memdim: Integer, dimW: Integer, w: Integer) extends Component {
	val io = new Bundle {
		val xdim    = UFix(INPUT, dimW)
		val rowEn   = Bits(INPUT, 1)
		val rowDin  = UFix(INPUT, w)
		val rowDout = UFix(OUTPUT, w)
	}
	val rowMem = Mem(memdim, seqRead = true){ UFix(width = w) } // Memory (FF version): depth=xdim, entry-width=w
	
	val addrw = log2Up(memdim) // address width for Mem
	val wptr  = Reg(resetVal = UFix(0, addrw)) // write address pointer
	val rptr  = Reg(resetVal = UFix(xwin, addrw)) // read address pointer, delay (xdim-xwin) cycles
	val dout = Reg(resetVal = UFix(0, w))     // read reg (squential)
	
	when (io.rowEn && io.xdim != UFix(0)) {
		rowMem(wptr) := io.rowDin // write value
		wptr := wptr + UFix(1)
		when (wptr === io.xdim) {wptr := UFix(0)}
              
		rptr := rptr + UFix(1)
		when (rptr === io.xdim) {rptr := UFix(0)}
		dout := rowMem(rptr) // delay (xdim-xwin) cycles, read value
	}
	io.rowDout := dout
}


class windowBuf5x5(maxImageWidth: Integer, dataWidth: Integer) extends Component {
  val windowSize = 25 // fixed 5-by-5 window
  val dimWidth = log2Up(maxImageWidth) // get bit-width of the image size number
  val io = new Bundle {
    val din = UFix(INPUT, dataWidth)
    val dout = Vec(windowSize) { UFix(dir = OUTPUT, width = dataWidth) }
    val load = Bits(INPUT, 1)
    val image_width = UFix(INPUT, dimWidth) // 2^n - 1
  }

  val wX1 = Array(0,0, 5,10,15,20) // xx,xx, w21,w31,w41,w51
  val wX5 = Array(0, 4, 9, 14, 19) // xx, w15,w25,w35,w45
  
  val ROW = new rowBuf(xwin = 5, memdim = maxImageWidth, dimW = dimWidth, w = dataWidth*4)  // Four-row buffer
  ROW.io.xdim   := Reg(io.image_width)
  ROW.io.rowEn  := Reg(~io.load) //enable by io.load=LOW
  ROW.io.rowDin := Cat(io.dout(wX1(2)), io.dout(wX1(3)), io.dout(wX1(4)), io.dout(wX1(5))).toUFix()
  // Write four rows: [w*4-1, w*3]--row1; [w*3-1, w*2]--row2; [w*2-1, w]--row3; [w-1, 0]--row4
  
  io.dout(windowSize-1) := Reg(io.din)
  for (i <- 0 until 24) {
  	io.dout(i) := Reg(io.dout(i+1))
  	if (i == wX5(4)) { io.dout(i) := ROW.io.rowDout(dataWidth - 1, 0          )} //row4
  	if (i == wX5(3)) { io.dout(i) := ROW.io.rowDout(dataWidth*2-1, dataWidth  )} //row3
  	if (i == wX5(2)) { io.dout(i) := ROW.io.rowDout(dataWidth*3-1, dataWidth*2)} //row2
  	if (i == wX5(1)) { io.dout(i) := ROW.io.rowDout(dataWidth*4-1, dataWidth*3)} //row1
  }

}



// Scala Unit Test for Window Buffer
class windowBufTest(c: windowBuf5x5) extends Tester(c, Array(c.io)) { //binds the tester to windowBuf and test its io
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
    val nd11 = (xdim-xwin)*(ywin-1)+xwin*ywin
    vars(c.io.load) = Bits(0)
    vars(c.io.image_width) = UFix(xdim-1)
    for (i <- 0 until nd11) { // delay cycles, no trace
      vars(c.io.din) = UFix(inputs(i))
      step(vars, isTrace = false)
    }
    for (ti <- 0 until xdim*nrow-nd11) { // start testing
    	vars(c.io.din) = UFix(inputs(ti+nd11))
    	for (i <- 0 until xwin){
         for (j <- 0 until ywin){
           vars(c.io.dout(i*5+j)) = UFix(inputs(xdim*i+ti+j));
         }
       }
    	allGood = step(vars) && allGood // steps the DUT and compare to expected values in vars
    }
    allGood // all TRUE -> pass
  }
}

}
