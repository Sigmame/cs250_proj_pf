package Work

import Chisel._
  
object Work {
  def main(args: Array[String]): Unit = {
    val mainArgs = args.slice(2, args.length)
    val res =
    args(0) match {
//      case "filter" =>
//        chiselMain( mainArgs, () => 
//            new convolutionFilter(maxImageWidth = 1024,
//                                  maxImageHeight = 1024,
//                                  dataWidth = 8, 
//                                  coeffWidth = 16,  // coefficient width (including sign bit)
 //                                 coeffFract = 12,   // number of bits to right of binary point
 //                                 pipeStages = args(1).toInt))
//      case "convolutionTest" =>
 //       chiselMainTest( mainArgs, () => 
  //          new convolution(windowSize = 25,
  //                          dataWidth = 8,
   //                         coeffWidth = 16,
    //                        coeffFract = 12) ) { c => new convolutionTest(c) }
//      case "partialDerivTest" =>
//        chiselMainTest( mainArgs, () => 
//            new partialDeriv(windowSize = 8,
//                            dataWidth = 16,
//                            pdWidth = 16) ) { c => new partialDerivTest(c) }
//        case "uvCalcTest" =>
//          chiselMainTest( mainArgs, () =>
//              new uvCalc(pdWidth = 16) ) { c => new uvCalcTest(c)}
      
//        case "uvAvgTest" =>
//          chiselMainTest( mainArgs, () =>
//              new uvAvg(windowSize = 9, dataWidth = 16) ) { c => new uvAvgTest(c)}
        case "windowBufTest" =>
            chiselMainTest( mainArgs, () =>
                new windowBuf5x5(imageWidth = 128, dataWidth = 8)) { c => new windowBuf5x5test(c)}
  
    }
  } 
}   

