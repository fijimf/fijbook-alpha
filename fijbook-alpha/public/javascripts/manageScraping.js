function setupManagerSocket(addr) {
    var socket = new WebSocket(addr);

    socket.onopen = function () {
        console.log("Socket opened");
        $("#btnRequestStatus").click(function () {
            socket.send("$COMMAND::STATUS")
        });
        $("#btnFullRebuild").click(function () {
            socket.send("$COMMAND::FULL_REBUILD")
        });
        $("#btnUpdateCurrent").click(function () {
            socket.send("$COMMAND::UPDATE_CURRENT")
        });
        $("#btnCancelTask").click(function () {
            socket.send("$COMMAND::CANCEL_TASK")
        });
        $("#btnCancelTaskList").click(function () {
            socket.send("$COMMAND::CANCEL_TASK_LIST")
        });
        socket.send("$COMMAND::STATUS");
    };

    socket.onmessage = function (msgEvent) {
        console.log(msgEvent.data);

        var badge = $("#status-badge");
        var s = JSON.parse(msgEvent.data);

        function createTaskRow(tsk) {
            var faClass = "fa fa-question";
            var txtColor = "text-primary";
            if (tsk.message === "Running") {
                faClass = "fa fa-check-circle";
                txtColor = "text-success";
            } else if (tsk.message === "Completed") {
                faClass = "fa fa-check-circle";
                txtColor = "text-info";
            } else if (tsk.message === "Failed") {
                faClass = "fa fa-times-circle";
                txtColor = "text-danger";
            } else if (tsk.message === "Aborted") {
                faClass = "fa fa-times-circle";
                txtColor = "text-warning";
            }
            return $("<tr>")
                .append($("<td>", {"class": txtColor}).append($("<span>", {"class": faClass})))
                .append($("<td>").text(tsk.id.substring(0, 6) + "..."))
                .append($("<td>", {"class": txtColor}).text(tsk.name))
                .append($("<td>").text(tsk.startedAt))
                .append($("<td>").text(tsk.completedAt))
                .append($("<td>").text(tsk.elapsedTime))
                .append($("<td>", {"id": tsk.id}).text(""));
        }

        if (s.type === "ready") {
            badge.text(s.status);
            if (s.status === "Unknown") {
                badge.attr("class", "badge")
            } else if (s.status === "Running") {
                badge.attr("class", "badge badge-blue")
            } else if (s.status === "Completed") {
                badge.attr("class", "badge badge-green")
            } else if (s.status === "Aborted") {
                badge.attr("class", "badge badge-orange")
            } else if (s.status === "Failed") {
                badge.attr("class", "badge badge-red")
            }
        } else if (s.type === "running") {
            badge.text("Running");
            badge.attr("class", "badge badge-blue");
            var tbody = $("#task-data");
            var runRow = createTaskRow(s.running);
            tbody.html(runRow);
            var completedTaskRows = $.map(s.completed, function (val, i) {
                return createTaskRow(val);
            });
            $.each(completedTaskRows, function (i, val) {
                tbody.append(val);
            });

        } else if (s.type === "progress") {
            var progressCell = $("#" + s.taskId);
            if (progressCell) {
                progressCell.text(s.progress)
            }
        } else {
            badge.text("Error");
            badge.attr("class", "badge badge-red");
        }
    };
}

