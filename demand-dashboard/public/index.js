$(function() {
  var now = new Date();

  var fifteenMinutesAgo = new Date();
  fifteenMinutesAgo.setMinutes(-15);

  $("#startDateTime").val(fifteenMinutesAgo.toJSON().slice(0,19));
  $("#endDateTime").val(now.toJSON().slice(0,19));

  $("#demandForm").submit(function(event) {
    event.preventDefault();
    var startDateTime = new Date($("#startDateTime").val());
    var endDateTime = new Date($("#endDateTime").val());

    var postConfig = {
      url: $("#demandForm").attr("action"),
      data: JSON.stringify({
        startDateTime: startDateTime,
        endDateTime: endDateTime
      }),
      contentType : 'application/json',
      success: function(data) {
        $("#output").text("Demand = " + data.demand);
      }
    };

    $.post(postConfig);
  });
});
