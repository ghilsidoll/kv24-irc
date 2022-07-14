package com.github.ghilsidoll.irc.controller

import akka.actor.typed.ActorSystem
import com.github.ghilsidoll.irc.actor.RootBehavior
import com.github.ghilsidoll.irc.actor.RootBehavior.{PostLogin, PostMessage}
import com.typesafe.config.ConfigFactory
import javafx.application.Platform
import javafx.event.{Event, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.control.{ChoiceBox, Label, ScrollPane, TextField}
import javafx.scene.image.ImageView
import javafx.scene.{Node, Scene}
import javafx.scene.layout.{BorderPane, VBox}
import javafx.stage.{Screen, Stage}
import javafx.scene.input.{KeyCode, KeyEvent}

import java.util.Objects

class ChatSceneController(private val login: String) {

  @FXML
  protected var chatContainer: VBox = _

  @FXML
  protected var chatPreviewContainer: VBox = _

  @FXML
  protected var chatScrollPane: ScrollPane = _

  @FXML
  protected var loginLabel: Label = _

  @FXML
  protected var mainScene: BorderPane = _

  @FXML
  protected var messageTextField: TextField = _

  @FXML
  protected var recipientChoiceBox: ChoiceBox[String] = _

  @FXML
  protected var sendMessageButton: ImageView = _

  private final var actorSystem: ActorSystem[RootBehavior.Command] = _

  def getLogin: String = {
    login
  }

  def addRecipient(recipientLogin: String): Unit = {
    recipientChoiceBox.getItems.add(recipientLogin)
  }

  def startup(port: Int, controller: ChatSceneController): Unit = {
    val config = ConfigFactory.parseString(s"""akka.remote.artery.canonical.port=$port
      akka.cluster.seed-nodes=["akka://chat@127.0.0.1:25251",
      "akka://chat@127.0.0.1:25252"]""")
      .withFallback(ConfigFactory.load("application.conf"))

    actorSystem = ActorSystem(RootBehavior(controller), "chat", config)
  }

  def loadScene(event: Event, loader: FXMLLoader): Unit = {
    val screenBounds = Screen.getPrimary.getVisualBounds
    val window = event.getSource.asInstanceOf[Node].getScene.getWindow

    window.setX((screenBounds.getWidth - 1000) / 2)
    window.setY((screenBounds.getHeight - 760) / 2)

    window.asInstanceOf[Stage].setScene(new Scene(loader.load(), 1000, 760))
  }

   def displayMessage(login: String, content: String, modifier: Int = -1): Unit = {
     Platform.runLater(() => {
       val loader: FXMLLoader = new FXMLLoader(Objects.requireNonNull(getClass.getResource(
         "/view/template/messageBoxScene.fxml")))
       val node: VBox = loader.load()
       loader.getController.asInstanceOf[MessageBoxSceneController].setContent(login, content, modifier)
       chatContainer.getChildren.add(node)

       chatScrollPane.setVvalue(1d)
       messageTextField.setText("")
       messageTextField.requestFocus()
     })
  }

  private def sendMessage(): Unit = {

    if (messageTextField.getText.nonEmpty) {
      val recipient: String = recipientChoiceBox.getValue
      actorSystem ! PostMessage(messageTextField.getText,
        if (recipient == null || recipient == "Group") "" else recipient)
      actorSystem ! PostLogin()
    }

    // TODO: add validation for message

  }

  def initialize(): Unit = {
    Platform.runLater(() => mainScene.requestFocus())

    mainScene.setOnMouseClicked(_ => mainScene.requestFocus())

    messageTextField.setOnKeyPressed(new EventHandler[KeyEvent]() {
      override def handle(event: KeyEvent): Unit = {
        if (event.getCode == KeyCode.ENTER) {
          sendMessage()
        }
      }
    })

    loginLabel.setText(login)

    recipientChoiceBox.setValue("Group")

    sendMessageButton.setOnMouseClicked(_ => {
      sendMessage()
    })
  }
}
