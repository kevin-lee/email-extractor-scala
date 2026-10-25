kevinInitialProcess = []

log = (message) ->
  console.log message if console?

class Page
  constructor: (@resultMessage = $("#resultMessage"), @message = $("#message"), @inputText = $("#inputText"), @resultArea = $("#resultArea"), @submitButton = $("#submitButton"), @clearButton = $("#clearButton")) ->

  removeClassWithPrefix = (prefix) ->
    (index, css) ->
      ((css.match ///
        #{prefix}
        [-]?
        \S*
        ///g) or []).join(' ')

  hideResultMessage: =>
    @resultMessage.text ""
    @resultMessage.addClass "hidden" unless @resultMessage.hasClass "hidden"
    @resultMessage.removeClass removeClassWithPrefix "alert"

  hideMessage: ->
    @message.text ""
    @message.addClass "hidden" unless @message.hasClass "hidden"

  enableOrDisableSubmitButton: ->
    if @inputText.val()
      @submitButton.prop "disabled", false
    else
      @submitButton.prop "disabled", true

  enableOrDisableClearButton: ->
    if @resultMessage.text() or @message.text() or @inputText.val() or @resultArea.val()
      @clearButton.prop "disabled", false
    else
      @clearButton.prop "disabled", true

  enableAll: ->
    @inputText.prop "disabled", false
    @enableOrDisableSubmitButton()
    @enableOrDisableClearButton()

  disableAll: ->
    @inputText.prop "disabled", true
    @submitButton.prop "disabled", true
    @clearButton.prop "disabled", true

  setUp: () =>
    @inputText.on "keyup keydown keypress focus blur change", (event) =>
      @enableOrDisableSubmitButton()
      @enableOrDisableClearButton()

    @clearButton.on "click", (event) =>
      @inputText.val ""
      @resultArea.text ""
      @hideResultMessage()
      @hideMessage()
      @enableOrDisableSubmitButton()
      @enableOrDisableClearButton()

    @resultArea.on "click", (event) =>
      @resultArea.select()
      return

    $.ajaxSettings.traditional = true
    $.ajaxSetup
      "type" : "POST"
      "dataType" : "json"
      "contentType" : "application/json"
      "headers" :
        "Csrf-Token" : $("meta[name='csrf-token']").attr("content")


    @submitButton.on("click", (event) =>
      @disableAll()
      @hideResultMessage()
      @hideMessage()
      @resultArea.text ""
      jsRoutes.controllers.EmailAddressExtractorController.extract().ajax({
        "data" : JSON.stringify {
              "inputValue": "#{@inputText.val()}"
            }
      }).done((response) => 
        if response.success
          @resultMessage.text response.message
          @resultMessage.removeClass "hidden" if @resultMessage.hasClass "hidden"
          @resultMessage.removeClass removeClassWithPrefix "alert"
          @resultMessage.addClass "alert alert-success"
          @resultArea.text(response.result)
          @hideMessage()
        else
          @message.text response.message
          @message.removeClass "hidden" if @message.hasClass "hidden"
          @hideResultMessage()
          @resultArea.text ""

        @enableAll()
      ).fail((jqXHR, textStatus, errorThrown) =>
        log "=============== ERROR =============== {"
        log "jqXHR: "
        log jqXHR
        log "-------------------------------------"
        log "textStatus: "
        log  textStatus
        log "-------------------------------------"
        log "errorThrown: "
        log errorThrown
        log "} ====================================="
        @message.html("[#{textStatus}] Something went wrong! Please try again later.<p /> Details: #{errorThrown}")
        @message.removeClass "hidden" if @message.hasClass "hidden"
        @hideResultMessage()
        @resultArea.text ""
        @enableAll()
      )
    )

kevinInitialProcess.push ->
  page = new Page
  page.setUp()

window.kevinInitialProcess = [] unless window.kevinInitialProcess
window.kevinInitialProcess = window.kevinInitialProcess.concat kevinInitialProcess
