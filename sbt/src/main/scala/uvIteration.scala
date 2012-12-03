package Work {

import Chisel._
import Node._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class uvIteration(doutWidth: Integer, fractWidth: Integer, imageWidth: Integer, memWidth: Integer, iterationNum: Integer) extends Component {
  val io = new Bundle {
//    val din   = Vec(windowSize) { UFix(dir = INPUT, width = dataWidth) }
    val Ex = Fix(INPUT, doutWidth)
    val Ey = Fix(INPUT, doutWidth)
    val Et = Fix(INPUT, doutWidth)
    val iterCount = UFix(INPUT, log2Up(iterationNum))
    val u_out = Fix(OUTPUT, doutWidth)
    val v_out = Fix(OUTPUT, doutWidth)
    val u_in = Fix(INPUT, doutWidth)
    val v_in = Fix(INPUT, doutWidth)
  }
  val winBuf3_u = new windowBuf3x3 (imageWidth, doutWidth, memWidth)
  val winBuf3_v = new windowBuf3x3 (imageWidth, doutWidth, memWidth)
  val uvCalculation = new uvCalc(doutWidth, fractWidth)
  val uvAverage = new uvAvg(9, doutWidth)
  val iterIs0 = io.iterCount === UFix(0)
  uvCalculation.io.Ex := io.Ex
  uvCalculation.io.Ey := io.Ey
  uvCalculation.io.Et := io.Et
  uvCalculation.io.uAvg := Mux(iterIs0, UFix(0), uvAverage.io.uAvg.toUFix())
  uvCalculation.io.vAvg := Mux(iterIs0, UFix(0), uvAverage.io.vAvg.toUFix())
  uvAverage.io.uin := winBuf3_u.io.dout 
  uvAverage.io.vin := winBuf3_v.io.dout
  winBuf3_u.io.din := io.u_in.toUFix() //uvCalculation.io.u.toUFix()
  winBuf3_v.io.din := io.v_in.toUFix()  //uvCalculation.io.v.toUFix()
  io.u_out := uvCalculation.io.u//winBuf3_u.io.dout(5)
  io.v_out := uvCalculation.io.v//winBuf3_v.io.dout(5)
  }

  }
