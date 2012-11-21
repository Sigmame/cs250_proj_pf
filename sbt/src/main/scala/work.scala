package Work

import Chisel._
  
object Work {
  def main(args: Array[String]): Unit = {
    val mainArgs = args.slice(2, args.length)
    val res =
    args(0) match {
        case "hsOptFlow" =>
            chiselMain( mainArgs, () => 
                new hsOptFlowTop(imageWidth = 1024,
                                 imageHeight = 1024,
                                 dataWidth = 8, 
                                 coeffWidth = 20,  // coefficient width (including sign bit)
                                 coeffFract = 16,   // number of bits to right of binary point
                                 pipeStages = args(1).toInt))

        case "partialDerivTest" =>
            chiselMainTest( mainArgs, () => 
                new partialDeriv(windowSize = 8,
                                 dataWidth = 16,
                                 pdWidth = 16) ) { c => new partialDerivTest(c) }

        case "uvCalcTest" =>
            chiselMainTest( mainArgs, () =>
                new uvCalc(pdWidth = 26, pdFrac = 16, 
                           dWidth = 48, pWidth = 44, dpFrac = 32,
                           uvWidth = 26, uvFrac = 16) ) { c => new uvCalcTest(c)}
      
        case "uvAvgTest" =>
            chiselMainTest( mainArgs, () =>
                new uvAvg(windowSize = 9, dataWidth = 16) ) { c => new uvAvgTest(c)}

        case "windowBuf5Test" =>
            chiselMainTest( mainArgs, () =>
                new windowBuf5x5(imageWidth = 128, dataWidth = 8)) { c => new windowBuf5x5test(c)}
  
        case "windowBuf2Test" =>
            chiselMainTest( mainArgs, () =>
                new windowBuf2x2x2(imageWidth = 128, dataWidth = 26, memWidth = 32)) { c => new windowBuf2x2x2test(c)}

        case "windowBuf3Test" =>
            chiselMainTest( mainArgs, () =>
                new windowBuf3x3(imageWidth = 128, dataWidth = 26, memWidth = 32)) { c => new windowBuf3x3test(c)}
    }
  } 
}   

