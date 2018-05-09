package com.fijimf.deepfij.book

import java.time.{Duration, LocalDateTime}
import java.time.format.DateTimeFormatter

import com.fijimf.deepfij.models.book.{App, Bettor, Closed, Open}
import org.scalatest.FlatSpec

import scala.util.{Failure, Success}

class AppSpec extends FlatSpec {

  "A new App" should "be empty" in {
    val app = App.create("Test App", LocalDateTime.now())
    assert(app.name == "Test App")
    assert(app.time.isBefore(LocalDateTime.now()))
    assert(app.seq.next() != app.seq.next())
    assert(app.bettors.isEmpty)
    assert(app.books.isEmpty)
  }

  "An App" should "allow a bettor to be added" in {
    val app = App.create("Test App", LocalDateTime.now())
    app.addBettor("Jim") match {
      case Success(a) => assert(a.bettors == List(Bettor("Jim", 0)))
      case Failure(t) => fail("Adding a bettor failed", t)
    }
  }

  "An App" should "allow multiple bettors to be added" in {
    val app = for {
      a <- App.create("Test App", LocalDateTime.now()).addBettor("Jim")
      b <- a.addBettor("Zeke", 100)
      c <- b.addBettor("Steve")
    } yield c
    app match {
      case Success(a) => assert(a.bettors == List(Bettor("Steve", 0), Bettor("Zeke", 100), Bettor("Jim", 0)))
      case Failure(t) => fail("Adding a bettor failed", t)
    }
  }

  "An App" should "enforce uniqueness of bettors" in {
    val app = for {
      a <- App.create("Test App", LocalDateTime.now()).addBettor("Jim")
      b <- a.addBettor("Zeke", 100)
      c <- b.addBettor("Jim", 200)
    } yield c
    app match {
      case Success(_) => fail("Should have failed on a duplicate name")
      case Failure(_) => //OK
    }
  }

  "An app" should "allow a game to be added" in {
    val d = LocalDateTime.parse("2018-03-12T19:30:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    val app = for {
      a <- App.create("Test App", LocalDateTime.now()).addGame("georgetown", "villanova", d)
    } yield a
    app match {
      case Success(a) =>
        assert(a.books.size == 1)
        val book = a.books.head
        assert(book.bets.isEmpty)
        assert(book.offers.isEmpty)
        assert(book.game.homeTeam == "georgetown")
        assert(book.game.awayTeam == "villanova")
        assert(book.game.date == d)
        assert(book.game.result.isEmpty)
      case Failure(t) => fail("Failed adding a game", t)
    }
  }

  "An app" should "prevent poorly formed games from being added" in {
    val d = LocalDateTime.parse("2018-03-12T19:30:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    val app = for {
      a <- App.create("Test App", LocalDateTime.now()).addGame("georgetown", "georgetown", d)
    } yield a
    app match {
      case Success(a) => fail("Poorly formed game was not prevented")
      case Failure(t) => //OK
    }
  }

  "An app" should "allow multiple games to be added" in {
    val d1 = LocalDateTime.parse("2018-03-12T19:30:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    val d2 = d1.plusHours(3)
    val d3 = d1.plusDays(1)
    val app = for {
      w <- App.create("Test App", LocalDateTime.now()).addGame("utah", "byu", d1)
      x <- w.addGame("georgetown", "villanova", d1)
      y <- x.addGame("st-johns", "providence", d2)
      z <- y.addGame("xavier", "seton-hall", d3)
    } yield z
    app match {
      case Success(a) =>
        assert(a.books.size == 4)
        a.books.foreach(book => {
          assert(book.bets.isEmpty)
          assert(book.offers.isEmpty)
          assert(book.game.result.isEmpty)
        })
        assert(a.books.map(book => book.game.homeTeam) == List("utah", "georgetown", "st-johns", "xavier").reverse)
        assert(a.books.map(book => book.game.awayTeam) == List("byu", "villanova", "providence", "seton-hall").reverse)
        assert(a.books.map(book => book.game.date) == List(d1, d1, d2, d3).reverse)
      case Failure(t) => fail("Failed adding a game", t)
    }

  }

  "An app" should "prevent duplicate games (identical)" in {
    val d1 = LocalDateTime.parse("2018-03-12T19:30:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    val app = for {
      w <- App.create("Test App", LocalDateTime.now())
        .addGame("utah", "byu", d1)
      x <- w.addGame("utah", "byu", d1)
    } yield x
    app match {
      case Success(a) => fail("Two teams cannot play each other more than once per day")
      case Failure(_) => //OK
    }

  }
  "An app" should "prevent duplicate games (swapped home/away)" in {
    val d1 = LocalDateTime.parse("2018-03-12T19:30:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    val app = for {
      w <- App.create("Test App", LocalDateTime.now())
        .addGame("utah", "byu", d1)
      x <- w.addGame("byu", "utah", d1)
    } yield x
    app match {
      case Success(a) => fail("Two teams cannot play each other more than once per day")
      case Failure(_) => //OK
    }
  }
  
  "An app" should "prevent duplicate games (same day different time)" in {
    val d1 = LocalDateTime.parse("2018-03-12T19:30:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    val d2 = d1.plusHours(3)
    val app = for {
      w <- App.create("Test App", LocalDateTime.now())
        .addGame("byu", "utah", d1)
      x <- w.addGame("byu", "utah", d2)
    } yield x
    app match {
      case Success(a) => fail("Two teams cannot play each other more than once per day")
      case Failure(_) => //OK
    }

  }
  
  "Added games" should "have the correct state based on the current time and window" in {
    val t = LocalDateTime.parse("2018-03-11T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    val app = App.create("Test App",t, Duration.ofDays(5),Duration.ofHours(1))
    
    //15 minutes away
    app.addGame("georgetown", "st-johns", t.plusMinutes(15)) match {
      case Success(a)=>assert(a.books.head.state==Closed)
      case Failure(t)=>fail(t)
    }
    
    //2 hours away
    app.addGame("georgetown", "st-johns", t.plusHours(2)) match {
      case Success(a)=>assert(a.books.head.state==Open)
      case Failure(t)=>fail(t)
    }
    
    //1 days away
    app.addGame("georgetown", "st-johns", t.plusDays(1)) match {
      case Success(a)=>assert(a.books.head.state==Open)
      case Failure(t)=>fail(t)
    }

    //8 days away
    app.addGame("georgetown", "st-johns", t.plusDays(8)) match {
      case Success(a)=>assert(a.books.head.state==Closed)
      case Failure(t)=>fail(t)
    }
    
    
    
      
    
  }
}
