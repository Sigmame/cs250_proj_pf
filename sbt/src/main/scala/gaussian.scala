package Work {

import Chisel._
import Node._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class gaussian(windowSize: Integer, dataWidth: Integer, fractWidth: Integer, doutWidth: Integer) extends Component {
  val io = new Bundle {
    val din   = Vec(windowSize) { UFix(dir = INPUT, width = dataWidth) }
    val dout = UFix(OUTPUT, doutWidth)
  }
  val p = Vec(windowSize){UFix(width=fractWidth+dataWidth)}
  val product = Vec(windowSize){UFix(width=fractWidth+dataWidth+1)}
  val add0  = Vec((windowSize-1)/2){UFix(width=fractWidth+dataWidth+1)}
  val add1  = Vec((windowSize-1)/4){UFix(width=fractWidth+dataWidth+1)}
  val add2  = Vec((windowSize-1)/8){UFix(width=fractWidth+dataWidth+1)}
  val add3  = Vec(2){UFix(width=fractWidth+dataWidth+1)}
  val coeff = Vec(windowSize){UFix(width=fractWidth)}
  // set coefficient 
  coeff(0) := UFix(1139)  
  coeff(1) := UFix(2279) 
  coeff(2) := UFix(2849)
  coeff(3) := UFix(2279)
  coeff(4) := UFix(1139)
  coeff(5) := UFix(2279)
  coeff(6) := UFix(5128)
  coeff(7) := UFix(6838)
  coeff(8) := UFix(5128)
  coeff(9) := UFix(2279)
  coeff(10) := UFix(2849)
  coeff(11) := UFix(6838)
  coeff(12) := UFix(8548)
  coeff(13) := UFix(6838)
  coeff(14) := UFix(2849)
  coeff(15) := UFix(2279)
  coeff(16) := UFix(5128)
  coeff(17) := UFix(6838)
  coeff(18) := UFix(5128)
  coeff(19) := UFix(2279)
  coeff(20) := UFix(1139)
  coeff(21) := UFix(2279)
  coeff(22) := UFix(2849)
  coeff(23) := UFix(2279)
  coeff(24) := UFix(1139)
  // multiply pixel data and coefficient
  for (i<-0 until windowSize){
    p(i) := coeff(i)*io.din(i)
  // sign extend
    product(i) = Cat(p(i)(fractWidth+dataWidth-1),p(i)(fractWidth+dataWidth-1,0))}
  for (i<-0 until (windowSize-1)/2){
    add0(i) := product(2*i) + product(2*i+1)}
  for (i<-0 until (windowSize-1)/4){
    add1(i) := add0(2*i)+add0(2*i+1)}
  for (i<-0 until (windowSize-1)/8){
    add2(i) := add1(2*i)+add1(2*i+1)}
  add3(0) := add2(0)+add2(1)
  add3(1) := add2(2)+product(windowSize-1)
  io.dout := add3(0)+add3(1) //temp //(coeffFract+dataWidth+1,0)

 }

class gaussianTest(c: gaussian) extends Tester(c, Array(c.io)) { //binds the tester to convolution and test its io
  defTests {
    var allGood = true // test result boolean: true->pass
    val vars    = new HashMap[Node, Node]() // mapping of test nodes to literals
    val rnd     = new Random() // random number generator
    val maxInt  = 255 // 4-bit wide integer
    val wid     = 8 //data bit width
    val window  = 25 //window size
    var cnt     = 0 // test counts
    val inputs  = new Array[Integer](window) // 3-by-3 test window
    val scale   = 115.0
    val coeffs  = Array(2/scale,4/scale,5/scale,4/scale,2/scale, 4/scale,9/scale,12/scale,9/scale,4/scale, 5/scale,12/scale,15/scale,12/scale,5/scale, 4/scale,9/scale,12/scale,9/scale,4/scale, 2/scale,4/scale,5/scale,4/scale,2/scale) // gaussian

    def convolve(co: Array[Double], data: Array[Integer])  =
    {
      var sum = 0.5
      for (i <- 0 until window) {
        sum += data(i)*co(i)
      }
      if (sum < 0) sum = 0
      if (sum > maxInt) sum = maxInt
     // sum = sum *65536
      sum.toInt
    } // get the convolution of the input

    var pos = 0.0
    for (cnt <- 0 until 1) { // run tests
      for (i <- 0 until window) {
        inputs(i) = rnd.nextInt(maxInt/8)
        vars(c.io.din(i)) = UFix(inputs(i), wid)
//        vars(c.io.coeff(i)) = UFix((coeffs(i)*4096).toInt, width = 16) // gaussian & laplace
//        if (coeffs(i) >= 0) {vars(c.io.coeff(i)) = UFix((coeffs(i)*4096).toInt, width = 16)} // gaussian & laplace
//        else {
//          pos = 65536 + coeffs(i)*4096
//          vars(c.io.coeff(i)) = UFix(pos.toInt, width = 16)
//        }
      } // generate random input window
      vars(c.io.dout) = UFix(convolve(coeffs, inputs))
      allGood = step(vars) && allGood // steps the DUT and compare to expected values in vars
    }
    allGood // all TRUE -> pass
  }
}
}
