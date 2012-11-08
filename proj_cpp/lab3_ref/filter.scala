package Work {

import Chisel._
import Node._

class convolutionFilter_io(windowSize: Integer, dataWidth: Integer, coeffWidth: Integer, dimWidth: Integer) extends Bundle() {
  val data_in        = UFix(INPUT, dataWidth)
  val frame_sync_in  = Bits(INPUT, 1)

  val config_load    = Bits(INPUT, 1)
  val coeff_in       = Fix(INPUT, coeffWidth)
  val image_width    = UFix(INPUT, dimWidth)
  val image_height   = UFix(INPUT, dimWidth)

  val data_out       = UFix(OUTPUT,dataWidth)
  val frame_sync_out = Bits(OUTPUT,1)
}

class convolutionFilter(maxImageWidth: Integer, maxImageHeight: Integer, dataWidth: Integer, coeffWidth: Integer, coeffFract: Integer, pipeStages : Integer) extends Component {
  val windowSize = 25 // fixed 5-by-5 window
  val dimWidth = scala.math.max(log2Up(maxImageWidth), log2Up(maxImageHeight)) // get bit-width of the image size number
  val io = new convolutionFilter_io(windowSize, dataWidth, coeffWidth, dimWidth);

  val control = new control(windowSize, maxImageWidth, maxImageHeight, dimWidth, coeffWidth, pipeStages)
  control.io.frame_sync_in := io.frame_sync_in
  control.io.image_width := io.image_width
  control.io.image_height := io.image_height
  control.io.config_load := io.config_load
  control.io.coeff_in    := io.coeff_in

  val winBuf = new windowBuf5x5(maxImageWidth, dataWidth)
  winBuf.io.load := io.config_load
  winBuf.io.image_width := io.image_width
  winBuf.io.din := io.data_in.toUFix()

  val convolver = new convolution_wrapper(windowSize, dataWidth, coeffWidth, coeffFract, pipeStages)
  convolver.io.din := winBuf.io.dout
  for (i <- 0 until windowSize) {
    convolver.io.coeff(i) := control.io.coeff_out(i).toUFix() // type conversion from Fix to UFix
  }

  // NOTE: you will need to delay the frame_sync_out, data select signal and
  // the output of the window buffer appropriately to account for the number of
  // pipeline stages in your desgin

  val dosel_buf = control.io.dout_select
  val win33_buf = Reg(winBuf.io.dout(12))
  val syncO_buf = control.io.frame_sync_out

  val dosel_out = Reg(dosel_buf)
  val win33_out = Reg(win33_buf)
  val syncO_out = Reg(syncO_buf)

  val dosel_outReg = Reg(Bits(width=1))
  val win33_outReg = Reg(UFix(width=dataWidth))
  val syncO_outReg = Reg(Bits(width=1))
  
  if (pipeStages == 1) {
    dosel_outReg := dosel_buf
    win33_outReg := win33_buf
    syncO_outReg := syncO_buf
  } else if (pipeStages == 2) {
    dosel_outReg := dosel_out
    win33_outReg := win33_out
    syncO_outReg := syncO_out
  } else {
    // add additional registers between xxxxx_outReg and xxxxx_out
    val dosel_pipe = Vec(pipeStages-2) { Reg() { Bits(width = 1) } }
    val win33_pipe = Vec(pipeStages-2) { Reg() { UFix(width = dataWidth) } }
    val syncO_pipe = Vec(pipeStages-2) { Reg() { Bits(width = 1) } }

    dosel_pipe(0) := dosel_out
    win33_pipe(0) := win33_out
    syncO_pipe(0) := syncO_out

    for (i <- 1 until pipeStages-2) {
      dosel_pipe(i) := dosel_pipe(i-1)
      win33_pipe(i) := win33_pipe(i-1)
      syncO_pipe(i) := syncO_pipe(i-1)
    }

    dosel_outReg := dosel_pipe(pipeStages-3)
    win33_outReg := win33_pipe(pipeStages-3)
    syncO_outReg := syncO_pipe(pipeStages-3)
  }


  io.data_out       := Reg(Mux(dosel_outReg, convolver.io.dout, win33_outReg)) // mid-pixel
  io.frame_sync_out := syncO_outReg
}

}
