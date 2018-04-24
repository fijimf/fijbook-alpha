function setupManagerSocket(addr) {
    var socket = new WebSocket(addr);

    socket.onopen = function () {
        console.log("Socket opened");
        $("#btnRequestStatus").off('click').on('click', function () {
            socket.send("$command::status")
        });
        $("#btnGenerateParquet").off('click').on('click', function () {
            socket.send("$command::generate_parquet")
        });
        $("#btnGenerateStats").off('click').on('click', function () {
            socket.send("$command::generate_stats")
        });
        $("#btnGenerateAll").off('click').on('click', function () {
            socket.send("$command::generate_all")
        });
        $("#btnTerminateCluster").off('click').on('click', function () {
            socket.send("$command::terminate_cluster")
        });
        socket.send("$command::status");
    };

    socket.onclose = function (event) {
        console.log('Socket is closed. Reconnect will be attempted in 1 500 millis.', event.reason);
        setTimeout(function () {
            setupManagerSocket(addr);
        }, 500);
    };

    socket.onmessage = function (msgEvent) {
        console.log(msgEvent.data);

        var badge = $("#status-badge");
        var s = JSON.parse(msgEvent.data);

        function createTaskRow(cl) {
            var faClass = "fa fa-question";
            var txtColor = "text-primary";

            if (cl.status === "STARTING") {
                faClass = "fa fa-check-circle";
                txtColor = "text-success";
            } else if (cl.status === "BOOTSTRAPPING") {
                faClass = "fa fa-check-circle";
                txtColor = "text-warning";
            } else if (cl.status === "RUNNING") {
                faClass = "fa fa-check-circle";
                txtColor = "text-warning";
            } else if (cl.status === "WAITING") {
                faClass = "fa fa-check-circle";
                txtColor = "text-warning";
            } else if (cl.status === "TERMINATING") {
                faClass = "fa fa-check-circle";
                txtColor = "text-warning";
            } else if (cl.status === "TERMINATED") {
                faClass = "fa fa-check-circle";
                txtColor = "text-info";
            } else if (cl.status === "TERMINATED_WITH_ERRORS") {
                faClass = "fa fa-times-circle";
                txtColor = "text-danger";
            }
            return $("<tr>")
                .append($("<td>", {"class": txtColor}).text(cl.name.substring(0,10)+"\u2026"))
                .append($("<td>").text(cl.status))
                .append($("<td>").text(cl.create))
                .append($("<td>").text(cl.ready))
                .append($("<td>").text(cl.end));
        }


        badge.text(s.status);
        badge.attr("class", "badge badge-blue");
        var tbody = $("#task-data");
      
        tbody.html("");
        var completedTaskRows = $.map(s.list, function (val, i) {
            return createTaskRow(val);
        });
        $.each(completedTaskRows, function (i, val) {
            tbody.append(val);
        });

 
    };
}

