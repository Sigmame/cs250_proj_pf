package Work {

import Chisel._
import Node._
import scala.collection.mutable.ArrayBuffer

class hsOptFlow_io(windowSize: Integer, dataWidth: Integer, coeffWidth: Integer, dimWidth: Integer, doutWidth: Integer) extends Bundle() {
  val data_in1       = UFix(INPUT, dataWidth)
  val data_in2       = UFix(INPUT, dataWidth)
  val frame_sync_in  = Bits(INPUT, 1)

//  val image_width    = UFix(INPUT, dimWidth)
//  val image_height   = UFix(INPUT, dimWidth)
  val data_out_u       = UFix(OUTPUT,doutWidth)  //doutWidth = 9.16 + sign = 26
  val data_out_v       = UFix(OUTPUT,doutWidth)  //doutWidth = 9.16 + sign = 26
  val frame_sync_out = Bits(OUTPUT,1)
}

class hsOptFlowTop(imageWidth: Integer, imageHeight: Integer, dataWidth: Integer, coeffWidth: Integer, coeffFract: Integer, doutWidth : Integer, iterationNum: Integer) extends Component {
  val windowSize = 25
  val dimWidth = scala.math.max(log2Up(imageWidth), log2Up(imageHeight))
  val io = new hsOptFlow_io(windowSize, dataWidth, coeffWidth, dimWidth, doutWidth);
 
// Control 
  val control = new control(imageWidth, imageHeight)
  control.io.frame_sync_in := io.frame_sync_in
  
// Two 5x5 windowBuf for gaussian filter
  val winBuf5x5_1 = new windowBuf5x5(imageWidth, dataWidth)
  val winBuf5x5_2 = new windowBuf5x5(imageWidth, dataWidth)
  winBuf5x5_1.io.din := io.data_in1
  winBuf5x5_2.io.din := io.data_in2
  val wbuf1_out = winBuf5x5_1.io.dout(12)
  val wbuf2_out = winBuf5x5_2.io.dout(12)

// Two Gaussian Filter (windowSize, dataWidth, coeffWidth, coeffFract)
  val gaussianF1 = new gaussian(25, 8, 16, 16, 25)
  val gaussianF2 = new gaussian(25, 8, 16, 16, 25)
  gaussianF1.io.din := winBuf5x5_1.io.dout 
  gaussianF2.io.din := winBuf5x5_2.io.dout 
// Two Mux for detecting edge
  val data_g1 = Mux(control.io.dout_select, gaussianF1.io.dout, wbuf1_out << UFix(coeffFract))
  val data_g2 = Mux(control.io.dout_select, gaussianF2.io.dout, wbuf2_out << UFix(coeffFract))

// One 2x2x2 windowBuf for partial derivative
// imageWidth, dataWidth, memWidth
  val winBuf2x2x2 = new windowBuf2x2x2(imageWidth, 26, 32)
  winBuf2x2x2.io.din1 := Cat(UFix(0,1),data_g1) //data_g1
  winBuf2x2x2.io.din2 := Cat(UFix(0,1),data_g2) //data_g2

// One partial Derivative module: windowSize, dataWidth, pdWidth
  val pDeriv = new partialDeriv(8, 26, 26)
  pDeriv.io.din := winBuf2x2x2.io.dout


// calculate uv: pdWidth, pdFrac, dpFrac, uvWidth, uvFrac
   val iterCalc =  new uvIteration(26, 26, imageWidth, doutWidth, 1024) 

//             val x = uvCalculation(2) // the second element
  iterCalc.io.Ex := pDeriv.io.Ex
  iterCalc.io.Ey := pDeriv.io.Ey
  iterCalc.io.Et := pDeriv.io.Et

  io.data_out_u := UFix(0)
  io.data_out_v := UFix(0)
  io.data_out_u := Reg(iterCalc.io.u_out.toUFix()) // for test bench debug
  io.data_out_v := Reg(iterCalc.io.v_out.toUFix()) // for test bench debug
  io.frame_sync_out := Reg(control.io.frame_sync_out)
}

}
