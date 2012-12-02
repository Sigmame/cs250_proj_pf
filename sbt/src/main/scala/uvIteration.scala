package Work {

import Chisel._
import Node._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class uvIteration(doutWidth: Integer, fractWidth: Integer, imageWidth: Integer, memWidth: Integer) extends Component {
  val io = new Bundle {
//    val din   = Vec(windowSize) { UFix(dir = INPUT, width = dataWidth) }
    val Ex = Fix(INPUT, doutWidth)
    val Ey = Fix(INPUT, doutWidth)
    val Et = Fix(INPUT, doutWidth)
    val u_out = Fix(OUTPUT, doutWidth)
    val v_out = Fix(OUTPUT, doutWidth)
    val u_in = Fix(INPUT, doutWidth)
    val v_in = Fix(INPUT, doutWidth)
  }
  val winBuf3_u = new windowBuf3x3 (imageWidth, doutWidth, memWidth)
  val winBuf3_v = new windowBuf3x3 (imageWidth, doutWidth, memWidth)
  val uvCalculation = new uvCalc(doutWidth, fractWidth)
  val uvAverage = new uvAvg(9, doutWidth)
  uvCalculation.io.Ex := io.Ex
  uvCalculation.io.Ey := io.Ey
  uvCalculation.io.Et := io.Et
  uvCalculation.io.uAvg := uvAverage.io.uAvg
  uvCalculation.io.vAvg := uvAverage.io.vAvg
  uvAverage.io.uin := winBuf3_u.io.dout(5) 
  uvAverage.io.vin := winBuf3_v.io.dout(5)
  winBuf3_u.io.din := UFix(0)//io.u_in.toUFix() //uvCalculation.io.u.toUFix()
  winBuf3_v.io.din := UFix(0)//io.v_in.toUFix()  //uvCalculation.io.v.toUFix()
  io.u_out := uvCalculation.io.u//winBuf3_u.io.dout(5)
  io.v_out := uvCalculation.io.v//winBuf3_v.io.dout(5)
  }

  }
