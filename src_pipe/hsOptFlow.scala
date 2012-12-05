package Work {

import Chisel._
import Node._
import scala.collection.mutable.ArrayBuffer

class hsOptFlow_io(windowSize: Integer, dataWidth: Integer, doutWidth: Integer) extends Bundle() {
  val data_in1       = UFix(INPUT, dataWidth)
  val data_in2       = UFix(INPUT, dataWidth)
  val frame_sync_in  = Bits(INPUT, 1)
  val data_out_u       = UFix(OUTPUT,doutWidth)  //doutWidth = 9.16 + sign = 26
  val data_out_v       = UFix(OUTPUT,doutWidth)  //doutWidth = 9.16 + sign = 26
  val frame_sync_out = Bits(OUTPUT,1)
}

class hsOptFlowTop(imageWidth: Integer, imageHeight: Integer, dataWidth: Integer, doutWidth : Integer, fractWidth : Integer, memWidth: Integer, iterationNum: Integer, pipeStages: Integer) extends Component {
  val windowSize = 25
  val io = new hsOptFlow_io(windowSize, dataWidth, doutWidth);
 
// Control 
  val control = new control(imageWidth, imageHeight, iterationNum)
  control.io.frame_sync_in := io.frame_sync_in
  
// Two 5x5 windowBuf for gaussian filter
  val winBuf5x5_1 = new windowBuf5x5(imageWidth, dataWidth)
  val winBuf5x5_2 = new windowBuf5x5(imageWidth, dataWidth)
  winBuf5x5_1.io.din := io.data_in1
  winBuf5x5_2.io.din := io.data_in2
  val wbuf1_out = winBuf5x5_1.io.dout(12)
  val wbuf2_out = winBuf5x5_2.io.dout(12)

// Two Gaussian Filter (windowSize, dataWidth, coeffWidth, coeffFract)
  val gaussianF1 = new gaussian(25, dataWidth, fractWidth, doutWidth-1)
  val gaussianF2 = new gaussian(25, dataWidth, fractWidth, doutWidth-1)
  gaussianF1.io.din := winBuf5x5_1.io.dout 
  gaussianF2.io.din := winBuf5x5_2.io.dout 
// Two Mux for detecting edge
  val data_g1 = Mux(control.io.dout_select, gaussianF1.io.dout, wbuf1_out << UFix(fractWidth))
  val data_g2 = Mux(control.io.dout_select, gaussianF2.io.dout, wbuf2_out << UFix(fractWidth))

// One 2x2x2 windowBuf for partial derivative
// imageWidth, dataWidth, memWidth
  val winBuf2x2x2 = new windowBuf2x2x2(imageWidth, doutWidth, memWidth)
  winBuf2x2x2.io.din1 := Cat(UFix(0,1),data_g1) //data_g1
  winBuf2x2x2.io.din2 := Cat(UFix(0,1),data_g2) //data_g2

// One partial Derivative module: windowSize, dataWidth, pdWidth
  val pDeriv = new partialDeriv(8, doutWidth)
  pDeriv.io.din := winBuf2x2x2.io.dout


// calculate uv: pdWidth, pdFrac, dpFrac, uvWidth, uvFrac
   val iterCalc =  new uvIteration(doutWidth, fractWidth, imageWidth, memWidth, iterationNum) 
//connect iteration calculation block
  iterCalc.io.Ex := pDeriv.io.Ex
  iterCalc.io.Ey := pDeriv.io.Ey
  iterCalc.io.Et := pDeriv.io.Et
  iterCalc.io.iterCount := control.io.iter_count
  //connect to uvMem for all the uv data
  val uvMemory = new uvMem(imageWidth, imageHeight, doutWidth, memWidth)
  iterCalc.io.u_in := uvMemory.io.dout_u
  iterCalc.io.v_in := uvMemory.io.dout_v
  uvMemory.io.uv_sync_out := control.io.first_frame_out
//data output  
  val dout_select_uv = Reg(Reg(Reg(Reg(Reg(Reg(Reg(control.io.dout_select_uv)))))))
  io.data_out_u := Reg(Mux(dout_select_uv, iterCalc.io.u_out, UFix(0))).toUFix 
  io.data_out_v := Reg(Mux(dout_select_uv, iterCalc.io.v_out, UFix(0))).toUFix
  uvMemory.io.din_u := io.data_out_u.toUFix()
  uvMemory.io.din_v := io.data_out_v.toUFix()
  val f_sync = Reg(control.io.frame_sync_out)
  io.frame_sync_out := Reg(Reg(Reg(Reg(Reg(Reg(Reg(f_sync))))))) //Reg(control.io.frame_sync_out)
}

}
