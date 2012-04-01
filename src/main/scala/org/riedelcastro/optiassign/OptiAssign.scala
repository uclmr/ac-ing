package org.riedelcastro.optiassign

import java.io.{File, InputStream}
import io.Source
import collection.mutable.HashMap
import gurobi._


/**
 * @author sriedel
 */
object OptiAssign {
  def main(args: Array[String]) {
    val google = loadReviewersFromGoogleCSV(new File("csv/google-reviewers.csv"))
    val softconf = loadReviewersFromSoftConfCSV(new File("csv/softconf-reviewers-unix.csv")).groupBy(_.email)
    val reviewers = google.map(r => r.copy(username = softconf(r.email).head.username))
    val papers = loadPapers(new File("csv/submissions.csv"))
    val bids = loadBids(new File("csv/bidInfo.csv"), papers, reviewers)
    optimize(reviewers, papers, bids)

    //load papers
    //load reviewers
    //load bids
    //optimize
    //write out to softconf format
  }

  //use 
  def loadReviewersFromGoogleCSV(in: File) = {
    val result = for (line <- Source.fromFile(in).getLines()) yield {
      val split = line.split(",", -1)
      val name = split(0)
      val email = split(1)
      val seniority = split(2)
      val region = split(3)
      val limit = split(10).toInt
      val batch = split(6)
      Reviewer(name, name, email, limit, Map("seniority" -> seniority, "region" -> region, "batch" -> batch))
    }
    result.toSeq
  }

  def loadReviewersFromSoftConfCSV(in: File) = {
    val result = for (line <- Source.fromFile(in).getLines()) yield {
      val split = line.split(",", -1)
      val username = split(0)
      val email = split(1)
      Reviewer(username, username, email, 0)
    }
    result.toSeq
  }

  def loadPapers(in: File) = {
    val result = for (line <- Source.fromFile(in).getLines()) yield {
      val split = line.split(",", -1)
      val id = split(0)
      Paper(id)
    }
    result.toSeq
  }

  def loadBids(in: File, papers: Seq[Paper], reviewers: Seq[Reviewer]) = {
    val lines = Source.fromFile(in).getLines()
    val first = lines.next()
    val names = first.split(",", -1).drop(1)
    val byName = reviewers.groupBy(_.name)
    val sorted = names.map(byName(_).head)
    val byId = papers.groupBy(_.id)
    val result = for (line <- lines) yield {
      val split = line.split(",", -1)
      val id = split(0)
      val paper = byId(id).head
      val bids = split.drop(1)
      val seq = for ((bid, index) <- bids.zipWithIndex) yield Bid(paper, sorted(index), bid)
      Bids(paper, seq)
    }
    result.toSeq
  }

  def optimize(reviewers: Seq[Reviewer], papers: Seq[Paper], bids: Seq[Bids]): Seq[Assignment] = {

    val env = new GRBEnv("optiassign.log")
    val ilp = new GRBModel(env)

    //one variable per assignment
    val pair2var = new HashMap[(Reviewer, Paper), GRBVar]
    val batch2var = new HashMap[(Paper, String), GRBVar]
    val pair2bid = bids.flatMap(b => b.bids).map(b => (b.reviewer, b.paper) -> b).toMap

    //create variables
    for (paper <- papers) {
      for (batch <- Seq("INV1")) {
        val batchVar = ilp.addVar(0.0, Double.PositiveInfinity, 1.0, GRB.CONTINUOUS, "batch(%d,%d)".format(paper.id, batch))
        batch2var(paper -> batch) = batchVar
      }
      for (reviewer <- reviewers) {
        val bid = pair2bid.get(reviewer -> paper)
        val coefficient = bid match {
          case Some(Bid(_, _, "1")) => 1.0
          case Some(Bid(_, _, "2")) => 1.0
          case Some(Bid(_, _, "3")) => 1.0
          case Some(Bid(_, _, "4")) => 1.0
          case _ => 0.0
        }
        if (coefficient > Double.NegativeInfinity) {
          val variable = ilp.addVar(0.0, 1.0, -coefficient, GRB.BINARY, "assign(%d,%d)".format(paper.id, reviewer.username))
          pair2var(reviewer -> paper) = variable
        }
      }
    }

    ilp.update()

    //mappings from reviewers/papers to all corresponding variables
    val varsByPaper = pair2var.groupBy(_._1._2)
    val varsByReviewer = pair2var.groupBy(_._1._1)

    //each paper needs at least 3 reviews
    for ((paper, vars) <- varsByPaper) {
      val expr = new GRBLinExpr()
      for (v <- vars) expr.addTerm(1.0, v._2)
      ilp.addConstr(expr, GRB.EQUAL, 3.0, "3For%s".format(paper.id))
    }

    //reviewers shouldn't have more papers than their limit
    for ((reviewer, vars) <- varsByReviewer; if (reviewer.limit < 5.0)) {
      val expr = new GRBLinExpr()
      for (v <- vars) expr.addTerm(1.0, v._2)
      ilp.addConstr(expr, GRB.LESS_EQUAL, reviewer.limit, "limitFor%s".format(reviewer.username))
    }

    //auxiliary variables for number of batch reviewers per paper
    for (((paper, batch), batchVar) <- batch2var) {
      val expr = new GRBLinExpr()
      expr.addTerm(-1.0, batchVar)
      for (((reviewer, _), paperVar) <- varsByPaper(paper); if (reviewer.fields("batch") == batch)) {
        expr.addTerm(1.0, paperVar)
      }
      ilp.addConstr(expr, GRB.EQUAL, 0.0, "batch(%s,%s)".format(paper.id, batch))
    }

    ilp.optimize()

    val result = for (((reviewer, paper), v) <- pair2var;
                      value = v.get(GRB.DoubleAttr.X);
                      if (value > 0.0)) yield {
      paper -> reviewer
    }

    result.groupBy(_._1).map(pair => Assignment(pair._1,pair._2.values.toSeq)).toSeq

  }

}

case class Reviewer(username: String, name: String, email: String, limit: Double, fields: Map[String, Any] = Map.empty)

case class Paper(id: String, fields: Map[String, Any] = Map.empty)

case class Bid(paper: Paper, reviewer: Reviewer, bid: String)

case class Bids(paper: Paper, bids: Seq[Bid])

case class Assignment(paper: Paper, reviewers: Seq[Reviewer])
