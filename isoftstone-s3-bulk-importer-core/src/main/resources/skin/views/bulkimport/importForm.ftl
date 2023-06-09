<html>
<head>

    <style>
        /* Reset rules */
        /* http://meyerweb.com/eric/tools/css/reset/ v2.0 | 20110126 */
        html, body, div, span, applet, object, iframe, h1, h2, h3, h4, h5, h6, p,
        blockquote, pre, a, abbr, acronym, address, big, cite, code, del, dfn,
        em, img, ins, kbd, q, s, samp, small, strike, strong, sub, sup, tt, var,
        b, u, i, center, dl, dt, dd, ol, ul, li, form, label, legend, table,
        caption, tbody, tfoot, thead, tr, th, td, article, aside, canvas,
        details, embed, figure, figcaption, footer, header, hgroup, menu, nav,
        output, ruby, section, summary, time, mark, audio, video {
            margin: 0;
            padding: 0;
            border: 0;
            font-size: 100%;
            font: inherit;
            vertical-align: baseline
        }

        fieldset {
            margin: 2px 0px 20px;
        }

        legend {
            font-weight: bold
        }

        /* HTML5 display-role reset for older browsers */
        article, aside, details, figcaption, figure, footer, header, hgroup,
        menu, nav, section, summary {
            display: block
        }

        body {
            line-height: 1
        }

        ol, ul {
            list-style: none
        }

        blockquote, q {
            quotes: none
        }

        blockquote:before, blockquote:after, q:before, q:after {
            content: '';
            content: none
        }

        table {
            border-collapse: collapse;
            border-spacing: 0
        }

        caption, th, td {
            text-align: left;
            font-weight: normal;
            vertical-align: middle
        }

        img, a img {
            border: none
        }

        /* Global styles */
        body {
            background-color: #f5f5f5;
            color: #555;
            margin: 0;
            padding: 0;
            font: 1em/1.4em 'Lucida Grande', Arial, Helvetica, Geneva, sans-serif;
            line-height: 1.4
        }

        h1, h2, h3, h4, h5, h6 {
            padding: .8em 0;
            color: #000;
            font-weight: bold
        }

        h1 {
            font-size: 1.5em;
            line-height: 1.3em
        }

        h2 {
            font-size: 1.3em
        }

        h3 {
            font-size: 1.1em
        }

        h4 {
            font-size: 1.08em
        }

        h5 {
            font-size: 1.05em
        }

        h6 {
            font-size: 1em
        }

        p {
            padding: 0 0 .7em
        }

        ol {
            margin: 0 0 .8em;
            padding: 0 0 0 1.2em;
            list-style-type: decimal
        }

        ol li {
            padding: .4em 0;
            font-size: .9em
        }

        a, a:visited {
            color: #06bbe5;
            text-decoration: none;
            transition: all 0.15s ease 0s
        }

        a:hover {
            color: #006ab1
        }

        a:hover, a:active {
            outline: 0
        }

        b, strong {
            font-weight: bold
        }

        pre, .code {
            font-family: "Menlo", "Consolas", "Courier New", monospace
        }

        pre, input[type="search"], input[type="text"], input[type="button"],
        textarea {
            background-color: #fff;
            border: 1px solid #ddd;
            border-radius: 3px;
            font-size: 103%;
            padding: .4em
        }

        pre:focus, input[type="search"]:focus, input[type="text"]:focus, input[type="button"]:focus,
        textarea:focus {
            border-color: #2e9cff;
            box-shadow: 0 0 3px #b5dcff;
            outline: 0 none
        }

        input[type="button"] {
            box-shadow: 0 -3px 0 rgba(0, 0, 0, 0.2) inset;
            font-family: Arial, sans-serif;
            font-weight: bold;
            cursor: pointer
        }

        pre {
            cursor: text
        }

        pre code {
            background: none;
            line-height: 1.6em
        }

        ::-webkit-input-placeholder {
            color: #747c88;
            font-weight: normal
        }

        ::-moz-placeholder {
            color: #747c88;
            font-weight: normal
        }

        :-ms-input-placeholder {
            color: #747c88;
            font-weight: normal
        }
        .hide {
            display: none;
        }

        /* Specific classes */
        .container {
            background-color: #fff;
            border-radius: 5px;
            border: 1px solid #ddd;
            padding: 1em 1.5em;
            margin: 3em auto;
            width: 960px
        }

        #msg {
            font-weight: bold;
            text-align: center;
            font-size: 12pt;
            background-color: #DDDDDD;
        }

        .explain {
            font-size: smaller;
            display: block;
        }

        ul.breadcrumb {
            padding: 10px 16px;
            list-style: none;
            background-color: #eee;
        }
        ul.breadcrumb li {
            display: inline;
        }
        ul.breadcrumb li+li:before {
            padding: 8px;
            color: black;
            content: "/\00a0";
        }
        ul.breadcrumb li a {
            color: #0275d8;
            text-decoration: none;
        }
        ul.breadcrumb li a:hover {
            color: #01447e;
            text-decoration: underline;
        }
    </style>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/jquery/3.1.1/jquery.min.js"></script>
    <script>
        var baseUrl;
        var started = false;
        var taskId;

        function doLog(msg) {
            $("#msg").html(msg);
        }

        function importCompleted() {
            started = false;
            doLog("Import Completed!");
            $("#runBtn").attr("disabled", false);
            $("#killBtn").css("display", "none");
        }

        function killImport() {
            doLog("Killing import ...");
            var killUrl = baseUrl + "tasks/" + taskId + "/kill";
            $.ajax({
                url : killUrl,
                type : "GET",
                success : function(data, textStatus, jqXHR) {
                    doLog("import process killed");
                    started = false;
                    $("#runBtn").attr("disabled", false);
                    $("#killBtn").css("display", "none");
                },
                error : function(jqXHR, textStatus, errorThrown) {
                    alert("Unable to kill import");
                }
            });

        }

        function waitForCompletion() {
            var pingUrl = baseUrl + "tasks/" + taskId + "/running";
            $.ajax({
                url : pingUrl,
                type : "GET",
                success : function(data, textStatus, jqXHR) {
                    if ("false" == data) {
                        importCompleted();
                    } else {
                        doLog("import in progress");
                        window.setTimeout(waitForCompletion, 5000);
                    }
                },
                error : function(jqXHR, textStatus, errorThrown) {
                    alert("Unable to check status");
                }
            });

        }

        function serializeImportForm() {
            var params = $('#importForm').serializeArray();
            var cleaned = [];
            params.forEach(function(o) {
                if ((o.name === "leafType" || o.name === "folderishType")) {
                    if (o.value !== "") {
                        cleaned.push(o);
                    }
                } else {
                    cleaned.push(o);
                }
            });
            return cleaned;
        }

        function startImport() {
            started = false;
            var params = serializeImportForm();//$('#importForm').serializeArray();

            var err = false;
            params.forEach(function(o) {
                if ((o.name === "inputPath")) {
                    var rootPath = $("#srcLoc").data("root");
                    if (o.value == "" || o.value == rootPath) {
                        err = true;
                    }
                }
            });

            if(err) {
                alert("make sure the input path is selected!");
                return ;
            }

            var formURL = baseUrl + "tasks/run";
            params.push({
                name : 'interactive',
                value : 'false'
            });
            $("#runBtn").attr("disabled", true);
            formURL = formURL + "?" + $.param(params);
            doLog("starting importer");
            $.ajax({
                url : formURL,
                type : "GET",
                success : function(data, textStatus, jqXHR) {
                    taskId = data.taskId;
                    doLog("Importer started, task id: " + data.taskId);
                    started = true;
                    $("#killBtn").css("display", "inline");
                    logTail();
                    window.setTimeout(waitForCompletion, 5000);
                },
                error : function(jqXHR, textStatus, errorThrown) {
                    doLog("Unable to start the importer");
                }
            });
        }

        function scrollIframe() {
            if ($("#logTail").length != 0) {
                var logContent = $('#logTail').contents();
                logContent.scrollTop(logContent.height());
            }
        }

        function logTail() {
            if ($("#logTail").length == 0) {
                var logTailIFrame = $("<iframe id='logTail' width='100%' height='300px'></iframe>");
                $("body").append(logTailIFrame);
            }

            $("#logTail").attr("src", baseUrl + "tasks/" + taskId + "/log");
            $("#logTail").on("load",scrollIframe);
            // window.setTimeout(scrollIframe, 500);
            if (started) {
                window.setTimeout(logTail, 3000);
            }
        }

        function createBreadcrumb(loc) {

            $("#srcLoc").empty();

            if(loc) {

                var separator = (loc.indexOf("\\")==-1)?"/":"\\";
                var locs = (loc==separator) ? [""]:loc.split(separator);
                var resetbutton = $('<input type="reset">').click(function(){
                    changeToBreadcrumb(locs.length);
                });
                $("#srcLoc").append(resetbutton);
                $.each(locs, function(i, l){
                    var li = $('<li></li>');
                    if(i==locs.length-1) {
                        li.data("path",loc);
                        li.data("length",locs.length);
                        var a = $('<a></a>').attr("href","javascript:changeToBreadcrumb("+locs.length+")").text(l);
                        li.append(a);
                    }
                    else {
                        li.text(l);
                    }
                    $("#srcLoc").append(li);
                });

                $("#srcLoc").data("root",loc);
                $("#srcLoc").data("path",loc);
                $("#srcLoc").data("length",locs.length);
                $("#srcLoc").data("separator",separator);

                $("#inputPath").val(loc);

                createSublocationChoices(loc);
            }
        }

        function addToBreadcrumb(subloc) {

            if(subloc) {
                var length = $("#srcLoc").data("length");
                var separator = $("#srcLoc").data("separator");
                var path = $("#srcLoc").data("path");
                if(!path.endsWith(separator)) {
                    path = path+separator;
                }
                path = path+subloc;
                var a = $('<a></a>').attr("href","javascript:changeToBreadcrumb("+(length+1)+")").text(subloc);
                var li = $('<li></li>').append(a).data("path",path).data("length",length+1);
                $("#srcLoc").append(li);

                $("#srcLoc").data("path",path);
                $("#srcLoc").data("length",length+1);

                $("#inputPath").val(path);

                createSublocationChoices(path);
            }
        }

        function changeToBreadcrumb(i) {

            var length = $("#srcLoc").data("length");
            var path = $("#srcLoc").data("path");

            $("#srcLoc").children("li:gt("+(i-1)+")").remove();
            var last = $("#srcLoc").children("li:last-child");
            $("#srcLoc").data("length",i);
            $("#srcLoc").data("path",last.data("path"));

            $("#inputPath").val(last.data("path"));

            createSublocationChoices(last.data("path"));
        }

        function createSublocationChoices(path) {

            $("#subLoc").empty();
            $("#subLoc").append($('<option></option>').val("").text("Select..."));
            $.getJSON(baseUrl+"source/sublocations?baseDir="+encodeURIComponent(path), function(result){
                $.each(result, function(i, loc){
                    var option = $('<option></option>').attr("value", loc).text(loc);
                    $("#subLoc").append(option);
                });
            });
        }

        $(document).ready(function() {
            baseUrl = document.location.href;
            var idx = baseUrl.indexOf("?");
            if (idx > 0) {
                baseUrl = baseUrl.substring(0, idx);
            }
            if (!(baseUrl.substring(baseUrl.length - 1) == "/")) {
                baseUrl = baseUrl + "/";
            }
            $("#importForm").attr("action", baseUrl);

            $.getJSON(baseUrl+"source/locations", function(result){

                createBreadcrumb(result.root);

            });

            $("#subLoc").change(function(){

                if($("#subLoc").val()!="") {
                    addToBreadcrumb($("#subLoc").val());
                }
            });
        });
    </script>

</head>
<body>

<div class="container">

    <div id="msg"></div>

    <form id="importForm" method="GET" enctype="application/x-www-form-urlencoded">

        <fieldset id="fileConfig">
            <legend>File Importer configuration</legend>

            <div>
                <label for="targetPath">Target path in Nuxeo repository:</label> <input
                        type="text" name="targetPath" readonly="readonly" value="${targetPath}" size="60">
                <p></p>
            </div>

            <div>
                <label for="srcLoc">Will import all files under the folder (on the server):</label>
                <ul id="srcLoc" class="breadcrumb"></ul>
                <div>
                    <label for="subLoc">Import from a subfolder (on the server)?</label>
                    <select id="subLoc"></select>
                    <p></p>
                </div>
                <div>
                    <input type="hidden" id="inputPath" name="inputPath" value="">
                    <p></p>
                </div>
            </div>
        </fieldset>

        <fieldset class="hide">
            <legend>Common configuration</legend>

            <div>
                <label for="batchSize">Batch size:</label> <input type="number"
                                                                  name="batchSize" placeholder="10" value="10" style="width: 5em">
                <p>
							<span class="explain">Number of documents imported in a
								single transaction</span>
                </p>
            </div>

            <div>
                <label for="nbThreads">Number of concurrent threads in the
                    importer process: </label> <input type="number" name="nbThreads"
                                                      placeholder="5" value="5">
                <p></p>
            </div>

            <div>
                <label for="transactionTimeout">Timeout (seconds) of the
                    transaction: </label> <input type="number" name="transactionTimeout"
                                                 placeholder="0" value="0">
                <p>
							<span class="explain">0 means "use the default value
								defined in the importer"</span>
                </p>
                <p></p>
            </div>

        </fieldset>
<#--        Enable Log For test-->
        <input type="hidden" name="enableLogging" value="true"/>

        <input type="button" id="runBtn" value="Run" onclick="startImport()" />
        <input type="button" id="killBtn" value="Stop import"
               onclick="killImport()" style="display: none" />

    </form>
</div>
</body>
</html>