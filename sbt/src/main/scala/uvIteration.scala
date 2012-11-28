package Work {

import Chisel._
import Node._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class uvIteration(pdWidth: Integer, uvWidth: Integer, imageWidth: Integer, doutWidth: Integer, memWidth: Integer) extends Component {
  val io = new Bundle {
//    val din   = Vec(windowSize) { UFix(dir = INPUT, width = dataWidth) }
    val Ex = Fix(INPUT, pdWidth)
    val Ey = Fix(INPUT, pdWidth)
    val Et = Fix(INPUT, pdWidth)
    val u_out = Fix(OUTPUT, uvWidth)
    val v_out = Fix(OUTPUT, uvWidth)
  }
  val winBuf3_u = new windowBuf3x3 (imageWidth, doutWidth, 1024)
  val winBuf3_v = new windowBuf3x3 (imageWidth, doutWidth, 1024)
  val uvCalculation = new uvCalc(26, 16, 16, 26, 16)
  val uvAverage = new uvAvg(9, 26)
  uvCalculation.io.Ex := io.Ex
  uvCalculation.io.Ey := io.Ey
  uvCalculation.io.Et := io.Et
  uvCalculation.io.uAvg := uvAverage.io.uAvg
  uvCalculation.io.vAvg := uvAverage.io.vAvg
  uvAverage.io.uin := winBuf3_u.io.dout(5) 
  uvAverage.io.vin := winBuf3_v.io.dout(5)
  winBuf3_u.io.din := uvCalculation.io.u.toUFix()
  winBuf3_v.io.din := uvCalculation.io.v.toUFix()
  io.u_out := winBuf3_u.io.dout(5)
  io.v_out := winBuf3_v.io.dout(5)
  }

  }
