package Work {

import Chisel._
import Node._

class hsOptFlow_io(windowSize: Integer, dataWidth: Integer, coeffWidth: Integer, dimWidth: Integer, doutWidth: Integer) extends Bundle() {
  val data_in1       = UFix(INPUT, dataWidth)
  val data_in2       = UFix(INPUT, dataWidth)
  val frame_sync_in  = Bits(INPUT, 1)

//  val image_width    = UFix(INPUT, dimWidth)
//  val image_height   = UFix(INPUT, dimWidth)
  val img_out1      = UFix(OUTPUT, doutWidth)
  val data_out       = UFix(OUTPUT,doutWidth)  //doutWidth = 9.16 + sign = 26
  val frame_sync_out = Bits(OUTPUT,1)
}

class hsOptFlowTop(imageWidth: Integer, imageHeight: Integer, dataWidth: Integer, coeffWidth: Integer, coeffFract: Integer, doutWidth : Integer) extends Component {
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
  io.img_out1 := data_g1 

// One 2x2x2 windowBuf for partial derivative
// imageWidth, dataWidth, memWidth
  val winBuf2x2x2 = new windowBuf2x2x2(imageWidth, 26, 32)
  winBuf2x2x2.io.din1 := data_g1 //data_g1
  winBuf2x2x2.io.din2 := data_g2 //data_g2

// One partial Derivative module: windowSize, dataWidth, pdWidth
  val pDeriv = new partialDeriv(8, 26, 26)
  pDeriv.io.din := winBuf2x2x2.io.dout

// calculate uv: pdWidth, pdFrac, dpFrac, uvWidth, uvFrac
  val uvCalculation = new uvCalc(26, 16, 16, 26, 16)
  val uvAverage = new uvAvg(9, 26)
  uvCalculation.io.Ex := pDeriv.io.Ex
  uvCalculation.io.Ey := pDeriv.io.Ey
  uvCalculation.io.Et := pDeriv.io.Et
  uvCalculation.io.uAvg := uvAverage.io.uAvg 
  uvCalculation.io.vAvg := uvAverage.io.vAvg

// calculate average uv:
  io.data_out := Reg(pDeriv.io.Ex.toUFix()) // for test bench debug
  io.frame_sync_out := Reg(control.io.frame_sync_out)
}

}
