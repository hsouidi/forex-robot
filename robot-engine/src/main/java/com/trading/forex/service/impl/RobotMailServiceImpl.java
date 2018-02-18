package com.trading.forex.service.impl;

import com.trading.forex.common.utils.AlgoUtils;
import com.trading.forex.connector.model.Position;
import com.trading.forex.model.Status;
import com.trading.forex.model.TradeHistoryResponse;
import com.trading.forex.service.RobotMailService;
import com.trading.forex.service.RobotReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@Service
@Slf4j
public class RobotMailServiceImpl implements RobotMailService {

    @Autowired
    private RobotReportService robotReportService;

    @Value("${mail.from}")
    private String from;

    @Value("${mail.to}")
    private String to;

    @Value("${mail.smtp.pwd}")
    private String pwd;

    @Value("${server.port}")
    private String port;

    @Override
    public void sendStatusMail(final String subject) {

        final Status status = robotReportService.status();
        final StringBuilder html = new StringBuilder(headerTemplate());
        // server adresse
        html.append("<a href=\"http://"+ AlgoUtils.getMyPublicIp() +":"+port+"/status\">Server Status Link </a>");
        // robot server status
        html.append(getRobotStatus(status.getMode(),
                status.getServerStatus(),
                status.getSolde(),
                status.getPositionProfit(),
                status.getPayout()));
        // stats
        html.append(getStats(status.getMaxPosition(), status.getMinPosition(), status.getMaxProfit(), status.getMaxLoss()));
        // opened position
        html.append(getOpenedPosition(status.getOpenedPositions()));
        // closed position
        html.append(getClosedPosition(status.getClosedTrades()));

        html.append("</body>\n" +
                "</html>\n");
        sendMail(html.toString(), subject+" - "+new SimpleDateFormat("yyyy/MM/dd").format(new Date()));

    }

    private String getClosedPosition(List<TradeHistoryResponse> closedTrades) {
        final StringBuilder result = new StringBuilder(
                "<h1>Closed positions</h1><br> <table class=\"responstable\">\n" +
                        "    <tbody>\n" +
                        "    <tr>\n" +
                        "        <th>Symbol</th>\n" +
                        "        <th>Way</th>\n" +
                        "        <th>P&L in PIP </th>\n" +
                        "        <th>Open Date</th>\n" +
                        "        <th>Close Date</th>\n" +
                        "\n" +
                        "    </tr>\n");

        final SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        closedTrades.forEach(trade -> {
            result.append("<tr  class=\"" + (trade.getPip() > 0 ? "green" : "red") + "\">\n" +
                    "        <td>" + trade.getSymbol() + "</td>\n" +
                    "        <td>" + trade.getWay() + "</td>\n" +
                    "        <td>" + trade.getPip() + "</td>\n" +
                    "        <td>" + format.format(trade.getInputDate()) + "</td>\n" +
                    "        <td>" + format.format(trade.getCloseDate()) + "</td>\n" +
                    "    </tr>\n");

        });
        result.append("</tbody>\n" +
                "</table>");
        return result.toString();

    }

    private String getOpenedPosition(List<Position> openedPositions) {
        final StringBuilder result = new StringBuilder(
                "<h1>Opened positions</h1><br> <table class=\"responstable\">\n" +
                        "    <tbody>\n" +
                        "    <tr>\n" +
                        "        <th>Symbol</th>\n" +
                        "        <th>Way</th>\n" +
                        "        <th>P&L in Euro </th>\n" +
                        "    </tr>\n" +
                        "\n");

        openedPositions.forEach(position -> {
            result.append("<tr  class=\"" + (position.getUnrealizedPL() > 0 ? "green" : "red") + "\">\n" +
                    "        <td>" + position.getSymbol() + "</td>\n" +
                    "        <td>" + (position.getShortValue() < 0 ? "SELL" : "BUY") + "</td>\n" +
                    "        <td>" + position.getUnrealizedPL() + "</td>\n" +
                    "     </tr>\n");
        });
        result.append("</tbody>\n" +
                "</table>");
        return result.toString();

    }

    private String getStats(Double maxPosition, Double minPosition, Double maxProfit, Double maxLoss) {
        final StringBuilder result = new StringBuilder(
                "<h1>Robot stats</h1><br> <table class=\"responstable\">\n" +
                        "    <tbody>\n" +
                        "    <tr>\n" +
                        "        <th> Max realized P&L in Euro</th>\n" +
                        "        <th> Min realized P&L in Euro</th>\n" +
                        "        <th> Max unrealized in Euro</th>\n" +
                        "        <th> Min unrealized in Euro</th>\n" +
                        "    </tr>\n" +
                        "    <tr>\n" +
                        "        <td class='green'> " + maxProfit + "</td>\n" +
                        "        <td class='red'> " + maxLoss + "</td>\n" +
                        "        <td class='green'> " + maxPosition + "</td>\n" +
                        "        <td class='red'> " + minPosition + "</td>\n" +
                        "    </tr>\n" +
                        "</tbody></table>");
        return result.toString();
    }

    private String getRobotStatus(String mode, Boolean serverStatus, Double solde, Double positionProfit, Double payout) {
        final StringBuilder result = new StringBuilder(
                "<h1>Robot server Status</h1><br> <table class=\"responstable\">\n" +
                        "    <thead>\n" +
                        "    <tr>\n" +
                        "        <th> Mode</th>\n" +
                        "        <th>Server Status</th>\n" +
                        "        <th> Session P&L in Euro</th>\n" +
                        "        <th> Unrealized P&L in Euro</th>\n" +
                        "        <th> Lot size</th>\n" +
                        "    </tr>\n" +
                        "    </thead>\n" +
                        "    <tbody>\n" +
                        "    <tr class=\"grey\">\n" +
                        "        <td class=\"" + (mode.equals("DEMO") ? "green" : "red") + "\"> " + mode + "</td>\n" +
                        "        <td class=\"" + (serverStatus ? "green" : "red") + "\"> " + (serverStatus ? "UP" : "DOWN") + "</td>\n" +
                        "        <td class=\"" + (solde>0 ? "green" : "red") + "\"> " + solde + "</td>\n" +
                        "        <td class=\"" + (positionProfit>0 ? "green" : "red") + "\"> " + positionProfit + "</td>\n" +
                        "        <td>" + payout + "</td>\n" +
                        "    </tr>\n" +
                        "    </tbody>\n" +
                        "</table>");

        return result.toString();
    }


    private String headerTemplate() {
        return "<html>\n" +
                "<head>\n" +
                "<style type=\"text/css\">\n" +
                ".red {\n" +
                "  background-color: #ff4444;\n" +
                "}\n" +
                "\n" +
                ".green {\n" +
                "  background-color: #baf326;\n" +
                "}\n" +
                ".grey {\n" +
                "  background-color: #EAF3F3;\n" +
                "}\n" +
                "\n" +
                ".responstable {\n" +
                "  margin: 1em 0;\n" +
                "  width: 100%;\n" +
                "  overflow: hidden;\n" +
                "  background: #FFF;\n" +
                "  color: #024457;\n" +
                "  border-radius: 10px;\n" +
                "  border: 1px solid #167F92;\n" +
                "}\n" +
                ".responstable tr {\n" +
                "  border: 1px solid #D9E4E6;\n" +
                "}\n" +
                ".responstable th {\n" +
                "  display: none;\n" +
                "  border: 1px solid #FFF;\n" +
                "  background-color: #167F92;\n" +
                "  color: #FFF;\n" +
                "  padding: 1em;\n" +
                "}\n" +
                ".responstable th:first-child {\n" +
                "  display: table-cell;\n" +
                "  text-align: center;\n" +
                "}\n" +
                ".responstable th:nth-child(2) {\n" +
                "  display: table-cell;\n" +
                "}\n" +
                ".responstable th:nth-child(2) span {\n" +
                "  display: none;\n" +
                "}\n" +
                ".responstable th:nth-child(2):after {\n" +
                "  content: attr(data-th);\n" +
                "}\n" +
                "@media (min-width: 480px) {\n" +
                "  .responstable th:nth-child(2) span {\n" +
                "    display: block;\n" +
                "  }\n" +
                "  .responstable th:nth-child(2):after {\n" +
                "    display: none;\n" +
                "  }\n" +
                "}\n" +
                ".responstable td {\n" +
                "  display: block;\n" +
                "  word-wrap: break-word;\n" +
                "  max-width: 7em;\n" +
                "}\n" +
                ".responstable td:first-child {\n" +
                "  display: table-cell;\n" +
                "  text-align: center;\n" +
                "  border-right: 1px solid #D9E4E6;\n" +
                "}\n" +
                "@media (min-width: 480px) {\n" +
                "  .responstable td {\n" +
                "    border: 1px solid #D9E4E6;\n" +
                "  }\n" +
                "}\n" +
                ".responstable th, .responstable td {\n" +
                "  text-align: left;\n" +
                "  margin: .5em 1em;\n" +
                "}\n" +
                "@media (min-width: 480px) {\n" +
                "  .responstable th, .responstable td {\n" +
                "    display: table-cell;\n" +
                "    padding: 1em;\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "body {\n" +
                "  padding: 0 2em;\n" +
                "  font-family: Arial, sans-serif;\n" +
                "  color: #024457;\n" +
                "  background: #f2f2f2;\n" +
                "}\n" +
                "\n" +
                "h1 {\n" +
                "  font-family: Verdana;\n" +
                "  font-weight: normal;\n" +
                "  color: #024457;\n" +
                "}\n" +
                "h1 span {\n" +
                "  color: #167F92;\n" +
                "}\n" +
                "\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>";
    }


    private void sendMail(final String html, String subject) {
        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true"); //TLS

        Session session = Session.getInstance(prop,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(from, pwd);
                    }
                });

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(to)
            );
            message.setSubject(subject);
            message.setContent(html, "text/html");

            Transport.send(message);

        } catch (MessagingException e) {
            log.error(e.getMessage(), e);
        }
    }
}
