package org.riedelcastro.optiassign

import java.io.{File, InputStream}


/**
 * @author sriedel
 */
object OptiAssign {
  def main(args: Array[String]) {
    //load papers
    //load reviewers
    //load bids
    //optimize
    //write out to softconf format
  }

  //use 
  def loadReviewers(in:File) = {
    Seq(Reviewer("test", 3.0, Map("seniority" -> 1)))
  }
  
  def loadPapers(in:File) = {
    Seq(Paper("123"))
  }

  def loadBids(in:File) = {
    Seq(Bid(null,null,"Eager"))
  }
  
  def optimize(reviewers:Seq[Reviewer], papers:Seq[Paper], bids:Seq[Bid]) : Seq[Assignment] = {
    //one variable per assignment
    //objective scores match with bids, and trust of reviewers per paper
    //constraints enforce reviews >= 3, and limits for reviewers
    Seq.empty
  }
  
}

case class Reviewer(username:String, limit:Double,  fields:Map[String,Any] = Map.empty)
case class Paper(id:String, fields:Map[String, Any] = Map.empty)
case class Bid(paper:Paper, reviewer:Reviewer, bid:String)
case class Assignment(paper:Paper,  reviewers:Seq[Reviewer])
