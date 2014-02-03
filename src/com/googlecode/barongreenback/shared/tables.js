BGB.namespace('tables').init = (function () {

    var initDataTables = function () {
        $.fn.dataTableExt.oSort['string-pre'] = function (value) {
            if (typeof value != "string")  value = value !== null && value.toString ? value.toString() : "";
            return value;
        };

        Date.setLocale("en-UK");

        $.fn.dataTableExt.aTypes.unshift(function (sData) {
            if (sData !== null && !sData.toString().match(/^\d+$/) && Date.create && Date.create(sData).isValid()) {
                return 'sugar';
            }
            return null;
        });

        jQuery.fn.dataTableExt.aTypes.unshift(function (sData) {
            sData = typeof sData.replace == 'function' ?
                sData.replace(/<[\s\S]*?>/g, "") : sData;
            sData = $.trim(sData);

            var sValidFirstChars = "0123456789-";
            var sValidChars = "0123456789.";
            var Char;
            var bDecimal = false;

            /* Check for a valid first char (no period and allow negatives) */
            Char = sData.charAt(0);
            if (sValidFirstChars.indexOf(Char) == -1) {
                return null;
            }

            /* Check all the other characters are valid */
            for (var i = 1; i < sData.length; i++) {
                Char = sData.charAt(i);
                if (sValidChars.indexOf(Char) == -1) {
                    return null;
                }

                /* Only allowed one decimal place... */
                if (Char == ".") {
                    if (bDecimal) {
                        return null;
                    }
                    bDecimal = true;
                }
            }

            return 'num-html';
        });

        $.extend($.fn.dataTableExt.oSort, {
            "sugar-pre": function (a) {
                return $.trim(a) == '' ? 0 : Date.create($.trim(a)).getTime();
            },

            "sugar-asc": function (a, b) {
                return a - b;
            },

            "sugar-desc": function (a, b) {
                return b - a;
            },
            "num-html-pre": function (a) {
                var x = String(a).replace(/<[\s\S]*?>/g, "");
                return parseFloat(x);
            },

            "num-html-asc": function (a, b) {
                return ((a < b) ? -1 : ((a > b) ? 1 : 0));
            },

            "num-html-desc": function (a, b) {
                return ((a < b) ? 1 : ((a > b) ? -1 : 0));
            }
        });

        $.fn.dataTableExt.oStdClasses['sSortAsc'] = 'headerSortDown';
        $.fn.dataTableExt.oStdClasses['sSortDesc'] = 'headerSortUp';
    };

    var updateFixedHeaderPosition = function () {
        var navbarHeight = $('.navbar').height();

        var scrolled = $(window).scrollTop();
        var resultsTop = $('div.dataTables_wrapper').offset().top;
        var offsetAfterScroll = (resultsTop - scrolled);

        var newTop = $('div.dataTables_wrapper').position().top;
        if (offsetAfterScroll < navbarHeight) {
            newTop += navbarHeight - offsetAfterScroll;
        }

        $('div.fixedHeader').css({
            'position': 'absolute',
            'top': newTop + 'px',
            'left': 'auto'
        });

        $('div.fixedHeader').slideDown('fast');
    };

    var registerAfterDrawCallback = function (table, funct) {
        var settings = table.fnSettings();
        settings.aoDrawCallback.unshift({
            'fn': funct,
            'sName': 'user'
        });
    };

    var interestedTables = function() {
        return $('table.results:not(.static)');
    }

    var initFixedHeader = function () {
        if (interestedTables().length == 0) {
            return;
        }

        var currentSort = $('th.headerSortUp, th.headerSortDown').map(function (collectionIndex, elem) {
            return [$(elem).index(), $(elem).hasClass('headerSortUp') ? 'desc' : 'asc'];
        });

        var clientSideSort = interestedTables().filter('.paged').length == 0;
        var resultsTable = interestedTables().dataTable({
            'bPaginate': false,
            'bFilter': false,
            'bSort': clientSideSort,
            'aaSorting': [currentSort.length == 0 ? [0, 'asc'] : currentSort],
            'bInfo': false,
            'bAutoWidth': false,
            'bSortClasses': false
        });

        interestedTables().filter(':not(.paged)').find('th a').click(function (event) {
            event.stopPropagation();
        });

        new FixedHeader(resultsTable, { offsetTop: $('.navbar').height()});

        /* This has to be done here because the FixedHeader plugin introduces some logic to reposition the header that we want to override. */
        registerAfterDrawCallback(resultsTable, updateFixedHeaderPosition);


        $('div.dataTables_wrapper').css('position', '');
        var startingTop = interestedTables().position().top;

        $('div.fixedHeader').css({
            'position': 'absolute',
            'top': startingTop,
            'left': 'auto'
        });

        $('div.dataTables_wrapper').after($('div.fixedHeader').detach());

        updateFixedHeaderPosition();
    };

    var registerWindowEvents = function () {

        var lastVerticalScroll = $(window).scrollTop();
        $(window).scroll(function() {
            var verticalScrollAmount = $(window).scrollTop();
            if (verticalScrollAmount === lastVerticalScroll) {
                $('div.fixedHeader').css({
                    'position': 'absolute',
                    'left': 'auto'
                });
                return;
            }
            lastVerticalScroll = verticalScrollAmount;
            $('div.fixedHeader').hide();
            clearTimeout($.data(this, 'scrollTimer'));
            $.data(this, 'scrollTimer', setTimeout(updateFixedHeaderPosition, 500));
        });

        var resizeHandler = function() {
            $('div.fixedHeader').hide();
            if ($("div.fixedHeader").length == 0) {
                return;
            }

            var tableWidth = interestedTables().outerWidth();

            $("div.fixedHeader").width(tableWidth);
            $("div.fixedHeader > table").width(tableWidth);

            var columnsWidths = [];
            $('.dataTables_wrapper th').each(function () {
                columnsWidths.push($(this).width());
            });

            var fixedHeaderCells = $('div.fixedHeader th')
            for (var count = 0; count < columnsWidths.length; count++) {
                $(fixedHeaderCells[count]).css('width', columnsWidths[count] + 'px');
            }
            updateFixedHeaderPosition();
        }

        $(window).resize(function () {
            setTimeout(resizeHandler, 500);
        });
    };

    return function () {
        initDataTables();
        initFixedHeader();
        registerWindowEvents();
    };
})();

$(document).ready(BGB.tables.init);
