package Work {

import Chisel._
import Node._

class control(imageWidth: Integer, imageHeight: Integer, iterationNum: Integer) extends Component {
  val io = new Bundle {
    val frame_sync_in  = Bits(INPUT, 1)
    //val gaussian_sync_out  = Bits(OUTPUT, 1)
    val frame_sync_out = Bits(OUTPUT, 1)
    val first_frame_out = Bits(OUTPUT, 1)
    val dout_select    = Bits(OUTPUT, 1)
    val dout_select_uv    = Bits(OUTPUT, 1)
    val iter_count = UFix(OUTPUT, log2Up(iterationNum))
  }
  val state = Reg(resetVal = UFix(0,3))
  val INITIAL = UFix(0,3)
  val BUF_GAUSSIAN     = UFix(1,3)
  val BUF_PD          = UFix(2,3)
  val CALC    = UFix(3,3)
  val count_x = Reg(resetVal = UFix(0,log2Up(imageWidth))) 
  val count_y = Reg(resetVal = UFix(0,log2Up(imageHeight))) 
  val coor_x = count_x - UFix(2)  //w22
  val coor_y = Reg(resetVal = UFix(0,log2Up(imageHeight)))
  val iterCount = Reg(resetVal = UFix(0,log2Up(iterationNum)))
  val isedge = coor_x ===UFix(0) ||coor_x===UFix(1)||coor_x===UFix(scala.math.pow(2,log2Up(imageWidth)).toInt -2) || coor_x ===UFix(scala.math.pow(2,log2Up(imageWidth)).toInt -1) || coor_y ===UFix(0) ||coor_y ===  UFix(1) || coor_y === UFix(imageHeight-2)||coor_y ===UFix(imageHeight-1)
  val isedge_uv = coor_x ===UFix(0) || coor_x ===UFix(scala.math.pow(2,log2Up(imageWidth)).toInt -1) || coor_y ===UFix(0) || coor_y === UFix(1) //||coor_y ===UFix(imageHeight-1)
  io.dout_select := ~isedge 
  io.dout_select_uv := Reg(~isedge_uv)
  io.frame_sync_out := Bits(0)
  io.first_frame_out := Bits(0)
  io.iter_count := iterCount 
  //INITIAL STATE
  when (state === INITIAL){
    when (io.frame_sync_in === Bits(1)){
      state := BUF_GAUSSIAN}      
  }
  //BUFFER STATE
  when (state === BUF_GAUSSIAN){
    count_x := Mux(count_x === UFix(imageWidth-1), UFix(0), count_x + UFix(1))
    count_y := Mux(count_x === UFix(imageWidth-1), count_y + UFix(1), count_y)
    coor_y := Mux(coor_x === UFix(scala.math.pow(2,log2Up(imageWidth)).toInt -1), coor_y + UFix(1), coor_y)
    when (count_x === UFix(2) && count_y === UFix(2)){
      state := BUF_PD
      coor_y := UFix(0)}
  }
  //BUFFER FOR PARTIAL DERIVATIVE
  when (state === BUF_PD){
    count_x := Mux(count_x === UFix(imageWidth-1), UFix(0), count_x + UFix(1))
    count_y := Mux(count_x === UFix(imageWidth-1), count_y + UFix(1), count_y)
    coor_y := Mux(coor_x === UFix(scala.math.pow(2,log2Up(imageWidth)).toInt -1), coor_y + UFix(1), coor_y)
    when (count_x === UFix(3) && count_y === UFix(3)){
      state := CALC 
      io.first_frame_out := UFix(1)
      when (iterCount === UFix(iterationNum-1)) {
        io.frame_sync_out := UFix(1)}
} }
  //Calculate (adjust this later)
  when (state === CALC){    //single cycle
    count_x := Mux(count_x === UFix(imageWidth-1), UFix(0), count_x + UFix(1))
    count_y := Mux(count_x === UFix(imageWidth-1), count_y + UFix(1), count_y)
    coor_y := Mux(coor_x === UFix(scala.math.pow(2,log2Up(imageWidth)).toInt -1), coor_y + UFix(1), coor_y)
      io.frame_sync_out := Bits(0)
      io.first_frame_out := Bits(0)
      when (count_x === UFix(imageWidth-1) && coor_y === UFix(imageHeight-1)){
        count_x := UFix(0,log2Up(imageWidth))
        count_y := count_y + UFix(1) 
        iterCount := iterCount + UFix(1)
        state:=BUF_GAUSSIAN}
  }

}
}

