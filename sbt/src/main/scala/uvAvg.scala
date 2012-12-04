package Work {

import Chisel._
import Node._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class uvAvg(windowSize: Integer, dataWidth: Integer) extends Component {
  val io = new Bundle {
    val uin   = Vec(windowSize) { Fix(dir = INPUT, width = dataWidth) }
    val vin   = Vec(windowSize) { Fix(dir = INPUT, width = dataWidth) }
    val uAvg = Fix(OUTPUT, dataWidth)
    val vAvg = Fix(OUTPUT, dataWidth)
  }
  val u6 = io.uin(1) + io.uin(3) + io.uin(5) + io.uin(7) 
  val u12 = io.uin(0) + io.uin(2) + io.uin(6) + io.uin(8)
  val u6_d = u6 * Fix(10922,17) //1/6
  val u12_d = u12 * Fix(5461,17) //1/12
  val uAvg_32 = u6_d + u12_d
  io.uAvg := uAvg_32(41,16)

  val v6 = io.vin(1) + io.vin(3) + io.vin(5) + io.vin(7) 
  val v12 = io.vin(0) + io.vin(2) + io.vin(6) + io.vin(8)
  val v6_d = v6 * Fix(10922,17) //1/6
  val v12_d = v12 * Fix(5461,17) //1/12
  val vAvg_32 = v6_d + v12_d
  io.vAvg := vAvg_32(41,16) 
 }

class uvAvgTest(c: uvAvg) extends Tester(c, Array(c.io)) { //binds the tester to convolution and test its io
  defTests {
    var allGood = true // test result boolean: true->pass
    val vars    = new HashMap[Node, Node]() // mapping of test nodes to literals
    val rnd     = new Random() // random number generator
    val wid     = 16 //data bit width
    val window  = 9 //window size
    var cnt     = 0 // test counts
    val inputs_u  = new Array[Integer](window) // 3-by-3 test window
    val inputs_v  = new Array[Integer](window) // 3-by-3 test window

    def uAvg(data_u: Array[Integer])  =
    {
      val u6 = data_u(1) + data_u(3) + data_u(5) + data_u(7) 
      val u12 = data_u(0) + data_u(2) + data_u(6) + data_u(8) 
      val uAvg = u6*0.1667 + u12*0.0833
      uAvg.toInt
    } // get the convolution of the input
    def vAvg(data_v: Array[Integer]) =
    {
      val v6 = data_v(1) + data_v(3) + data_v(5) + data_v(7) 
      val v12 = data_v(0) + data_v(2) + data_v(6) + data_v(8) 
      val vAvg = v6*0.1667 + v12*0.0833
      vAvg.toInt
    }
    for (cnt <- 0 until 20) { // run tests
      for (i <- 0 until window) {
        inputs_u(i) = rnd.nextInt(255)
        inputs_v(i) = rnd.nextInt(255)
        vars(c.io.uin(i)) = UFix(inputs_u(i), wid)
        vars(c.io.vin(i)) = UFix(inputs_v(i), wid)
      } // generate random input window
      vars(c.io.uAvg) = UFix(uAvg(inputs_u))
      vars(c.io.vAvg) = UFix(vAvg(inputs_v))
      allGood = step(vars) && allGood // steps the DUT and compare to expected values in vars
    }
    allGood // all TRUE -> pass
  }
}
}
