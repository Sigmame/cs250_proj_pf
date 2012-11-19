package Work {

import Chisel._
import Node._

class control(imageWidth: Integer, imageHeight: Integer) extends Component {
  val io = new Bundle {
    val frame_sync_in  = Bits(INPUT, 1)
    val frame_sync_out = Bits(OUTPUT, 1)
    val dout_select    = Bits(OUTPUT, 1)
  }
  val state = Reg(resetVal = UFix(0,2))
  val INITIAL = UFix(0,2)
  val BUF     = UFix(1,2)
  val CALC    = UFix(2,2)
  val count = Reg(resetVal = UFix(0,14)) 
  io.dout_select :=Bits(1,1)
  val coor = count - UFix(imageWidth+1)  //w22
  val x = coor(6,0)
  val y = coor(13,7)   //parametrize this later
  val isedge = x ===UFix(0) ||x===UFix(1)||x===UFix(2)|| x ===UFix(imageWidth-1) || y ===UFix(0) || y ===UFix(imageWidth-1)
  io.dout_select := ~isedge 

  //INITIAL STATE
  when (state === INITIAL){
    when (io.frame_sync_in === Bits(1)){
      state := BUF
      count := UFix(0)}      
  }
  //BUFFER STATE
  when (state === BUF){
    io.dout_select := Mux(isedge, UFix(1), UFix(0))
    when (count != UFix(imageWidth+1)){
      count := count + UFix(1)}
    when (count === UFix(imageWidth+1)){
      state := CALC
      count := count + UFix(1)
      io.frame_sync_out := UFix(1)}
  }
  //Calculate (adjust this later)
  when (state === CALC){    //single cycle
      io.dout_select := Mux(isedge, UFix(1), UFix(0))
      io.frame_sync_out := Bits(0)
      count := count + UFix(1)
      when (count === UFix(imageWidth*imageHeight-1)){
      count := UFix(0,14)
      state:=BUF}
  }

}
}

