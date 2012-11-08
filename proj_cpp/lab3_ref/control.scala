package Work {

import Chisel._
import Node._

class control(windowSize: Integer, maxImageWidth: Integer, maxImageHeight: Integer, dimWidth : Integer, coeffWidth: Integer, pipeStages : Integer) extends Component {
  val io = new Bundle {
    val frame_sync_in  = Bits(INPUT, 1)
    val config_load    = Bits(INPUT, 1)
    val coeff_in       = Fix(INPUT, coeffWidth)
    val image_width    = UFix(INPUT, dimWidth)
    val image_height   = UFix(INPUT, dimWidth)

    val frame_sync_out = Bits(OUTPUT, 1)
    val dout_select    = Bits(OUTPUT, 1)
    val coeff_out      = Vec(windowSize) { Fix(dir = OUTPUT, width = coeffWidth) }
  }

	// Define state reg
	val s_buff :: s_load :: s_work :: Nil = Enum(3){ UFix() }
	val state = Reg(resetVal = s_buff)
	
	// Control aux
	val coeffReg = Vec(windowSize) { Reg(resetVal = Fix(0, coeffWidth)) } //coeff regs
	val i_img = Reg(resetVal = UFix(0, 2)) // number of images counter
	val d_winBuf = Reg(resetVal = UFix(maxImageWidth))      // din(0,0) delayed version get to winBuf[12]
	val x_loc = Reg(resetVal = UFix(0, dimWidth))    // pixel location indicator
	val y_loc = Reg(resetVal = UFix(0, dimWidth))
	
	def edge(x: UFix, y: UFix, w: UFix, h: UFix) = {
		val eX = (x === UFix(0)) || (x === UFix(1)) || (x === w-UFix(1)) || (x === w)
		val eY = (y === UFix(0)) || (y === UFix(1)) || (y === h-UFix(1)) || (y === h) 
		eX || eY  //perimeter
	}
	val dsel_reg = ~edge(x_loc, y_loc, io.image_width, io.image_height) //edge=true gives Bits(1)
	val sync_reg = Reg((state === s_work) && (x_loc === UFix(0)) && (y_loc === UFix(0)) && i_img != UFix(0)) //start to output dout(0,0)

	// FSM
	when (state === s_load) {
		when (io.config_load){  //load coeff
			coeffReg(0) := io.coeff_in
			for (i <- 1 until windowSize) {
				coeffReg(i) := coeffReg(i-1)
			} // shift register
			state := s_load
		}
		.elsewhen (~io.config_load && io.frame_sync_in) {
			i_img := UFix(1) // load 1st image
			d_winBuf := io.image_width+io.image_width+UFix(4)  //reset counter
			state := s_buff
		}
	}
	.elsewhen (state === s_buff) {
		when (io.config_load) {
			coeffReg(0) := io.coeff_in
			for (i <- 1 until windowSize) {
				coeffReg(i) := coeffReg(i-1)
			} // shift register
			state := s_load
		}
		.elsewhen (~io.config_load && ~io.frame_sync_in && d_winBuf != UFix(0)){
			d_winBuf := d_winBuf - UFix(1)  //wait for window buffering delay
			state := s_buff
		}
		.elsewhen (~io.config_load && ~io.frame_sync_in && d_winBuf === UFix(0) && i_img != UFix(0)){
			x_loc := UFix(0)
			y_loc := UFix(0)
			state := s_work  //start calculation
		}
	}
	.elsewhen (state === s_work) { 
		when (io.frame_sync_in) { i_img := i_img + UFix(1) }
		when (x_loc === io.image_width-UFix(1) && y_loc === io.image_height) {
			i_img := i_img - UFix(1)
			d_winBuf := UFix(0)
			state := s_buff // reset final pixel
		}
		.otherwise {  //scan pixels
			when (x_loc === io.image_width){ //end of line
				x_loc := UFix(0)
				y_loc := y_loc + UFix(1)
			}
			.otherwise{
				x_loc := x_loc + UFix(1)
				y_loc := y_loc
			}
			state := s_work
		}
  	}
  
  
  io.coeff_out := coeffReg
  io.dout_select := dsel_reg //delay +1  
  io.frame_sync_out := sync_reg //delay +1
}

}
