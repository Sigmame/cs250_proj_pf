package Work {

import Chisel._
import Node._

class uvMem(imageWidth: Integer, imageHeight: Integer, doutWidth: Integer, memWidth: Integer) extends Component {
  val io = new Bundle {
    val uv_sync_out = Bits(INPUT, 1)
    val din_u = UFix(INPUT, doutWidth)
    val din_v = UFix(INPUT, doutWidth)
    val dout_u =  UFix(OUTPUT, doutWidth)
    val dout_v =  UFix(OUTPUT, doutWidth)
  }
  //instantiate SRAM 1024x32 (4*8bit)
  val uMem = Mem(imageWidth*imageHeight,seqRead=true){UFix(width=memWidth)}
  val vMem = Mem(imageWidth*imageHeight,seqRead=true){UFix(width=memWidth)}
  val readOut_u = Reg(resetVal = UFix(0,doutWidth))
  val readOut_v = Reg(resetVal = UFix(0,doutWidth))
  val count = Reg(resetVal = UFix(0,log2Up(imageWidth*imageWidth)))
  val state = Reg(resetVal = UFix(0,1))
  val WAIT = UFix(0,1)
  val STORE = UFix(1,1)

  when (state === WAIT){
    when (io.uv_sync_out === Bits(1)){
      state := STORE}}
  when (state === STORE){
    when (count != UFix(imageWidth * imageHeight)){
      count := count + UFix(1)}
    when (count === UFix(imageWidth * imageHeight)){
      count := UFix(0)}
  }
  //Write
  uMem(count) := io.din_u
  vMem(count) := io.din_v
  //Read
  readOut_u := uMem(count+UFix(imageWidth+1))
  readOut_v := vMem(count+UFix(imageWidth+1))
  io.dout_u := readOut_u(doutWidth-1,0) 
  io.dout_v := readOut_v(doutWidth-1,0)
}
}

