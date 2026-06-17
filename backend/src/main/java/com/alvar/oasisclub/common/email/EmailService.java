package com.alvar.oasisclub.common.email;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.util.Locale;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EmailService {

  private static final Logger log = LoggerFactory.getLogger(EmailService.class);
  private static final DateTimeFormatter RESERVATION_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
  private static final DateTimeFormatter RESERVATION_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("HH:mm");

  private final EmailDeliveryService emailDeliveryService;

  
  
  

  @Async
  public void sendPasswordResetEmail(String toEmail, String resetLink) {
    String subject = "Oasis Club | Recuperar contraseña";
    String plainText = buildResetEmailText(resetLink);

    String content = buildEmailTitle("Recuperar", "tu acceso")
        + buildEmailParagraph("""
            Hemos recibido una solicitud para restablecer la contraseña de tu cuenta.
            Pulsa el botón inferior para crear una nueva clave de acceso de forma segura.
            """)
        + buildCtaButton(resetLink, "Restablecer contraseña")
        + buildEmailFootnote(
            "Este enlace caducará en 30 minutos. Si no has solicitado este cambio, ignora este correo.");

    sendEmail(toEmail, subject, plainText, wrapEmailLayout(content), "password-reset");
  }

  @Async
  public void sendNewEventNotificationEmail(
      String toEmail,
      String userName,
      String eventTitle,
      String eventDescription,
      java.time.LocalDate eventDate,
      java.time.LocalTime startTime,
      java.time.LocalTime endTime,
      String category
  ) {
    String subject = "Oasis Club | Nuevo evento: " + eventTitle;
    String plainText = """
        OASIS CLUB

        Nuevo evento disponible

        Hola, %s.

        Hemos publicado un nuevo evento en el club:

        %s
        Fecha: %s
        Horario: %s – %s
        %s

        Reserva tu plaza desde la sección de Eventos.

        El equipo de Oasis Club.
        """.formatted(
            userName,
            eventTitle,
            formatReservationDate(eventDate),
            formatReservationTime(startTime),
            formatReservationTime(endTime),
            eventDescription != null && !eventDescription.isBlank() ? "\n" + eventDescription : ""
        );

    String detailsRows = buildDetailRow("Categoría", category)
        + buildDetailRow("Fecha", formatReservationDate(eventDate))
        + buildDetailRow("Horario", formatReservationTime(startTime) + " – " + formatReservationTime(endTime), true);

    String descriptionBlock = (eventDescription != null && !eventDescription.isBlank())
        ? buildEmailParagraph("<span style=\"font-style: italic;\">%s</span>".formatted(escapeHtml(eventDescription)))
        : "";

    String content = buildEmailTitle("Nuevo", "evento")
        + buildEmailParagraph("""
            Estimado/a <span style="font-weight: 500; color: #111111;">%s</span>,<br><br>
            Hemos publicado un nuevo evento en
            <span style="font-weight: 500; color: #111111;">Oasis Club</span>.
            Te avisamos por ser suscriptor del newsletter para que puedas reservar tu plaza con antelación.
            """.formatted(escapeHtml(userName)))
        + "<h2 style=\"font-family: 'Playfair Display', Georgia, serif; font-size: 22px; font-weight: 400; font-style: italic; color: #0B2118; text-align: center; margin: 0 0 24px;\">"
        + escapeHtml(eventTitle) + "</h2>"
        + descriptionBlock
        + buildDetailsTable(detailsRows)
        + buildEmailFootnote("Plazas limitadas. Inscríbete pronto desde la sección de Eventos.");

    sendEmail(toEmail, subject, plainText, wrapEmailLayout(content), "new-event-notification");
  }

  @Async
  public void sendWelcomeEmail(String toEmail, String userName) {
    String subject = "Oasis Club | Bienvenido";
    String plainText = buildWelcomeEmailText(userName);

    String content = buildEmailTitle("Bienvenido", "a Oasis Club")
        + buildEmailParagraph("""
            Estimado/a <span style="font-weight: 500; color: #111111;">%s</span>,<br><br>
            Tu cuenta ha sido activada correctamente. Ya puedes acceder a la plataforma para
            gestionar tus reservas, explorar nuestras instalaciones y disfrutar de la experiencia
            premium que ofrecemos.
            """.formatted(escapeHtml(userName)))
        + buildEmailFootnote("Tu historia en Oasis Club acaba de comenzar.");

    sendEmail(toEmail, subject, plainText, wrapEmailLayout(content), "welcome");
  }

  @Async
  public void sendReservationConfirmedEmail(
      String toEmail,
      String userName,
      String sport,
      String courtName,
      LocalDate date,
      LocalTime time
  ) {
    String subject = "Oasis Club | Reserva confirmada";
    String plainText = buildReservationConfirmedEmailText(userName, sport, courtName, date, time);

    String content = buildEmailTitle("Reserva", "Confirmada")
        + buildEmailParagraph("""
            Estimado/a <span style="font-weight: 500; color: #111111;">%s</span>,<br><br>
            Nos complace confirmar que tu reserva ha sido registrada con éxito. A continuación,
            los detalles de la misma.
            """.formatted(escapeHtml(userName)))
        + buildReservationDetailsHtml(sport, courtName, date, time)
        + buildEmailFootnote("Te esperamos en el club.");

    sendEmail(toEmail, subject, plainText, wrapEmailLayout(content), "reservation-confirmed");
  }

  @Async
  public void sendReservationCancelledEmail(
      String toEmail,
      String userName,
      String sport,
      String courtName,
      LocalDate date,
      LocalTime time
  ) {
    String subject = "Oasis Club | Reserva cancelada";
    String plainText = buildReservationCancelledEmailText(userName, sport, courtName, date, time);

    String content = buildEmailTitle("Reserva", "Cancelada")
        + buildEmailParagraph("""
            Estimado/a <span style="font-weight: 500; color: #111111;">%s</span>,<br><br>
            Te confirmamos que tu reserva ha sido cancelada correctamente. Estos eran sus detalles:
            """.formatted(escapeHtml(userName)))
        + buildReservationDetailsHtml(sport, courtName, date, time)
        + buildEmailFootnote("Puedes crear una nueva reserva cuando quieras desde tu perfil.");

    sendEmail(toEmail, subject, plainText, wrapEmailLayout(content), "reservation-cancelled");
  }

  @Async
  public void sendMaintenanceCancellationEmail(
      String toEmail,
      String userName,
      String sport,
      String courtName,
      LocalDate date,
      LocalTime time,
      boolean refunded
  ) {
    String subject = "Oasis Club | Reserva cancelada por mantenimiento";
    String refundNote = refunded
        ? "El importe de tu reserva será reembolsado automáticamente en los próximos 5-10 días hábiles."
        : "";
    String plainText = """
        OASIS CLUB

        Reserva cancelada por mantenimiento

        Hola, %s.

        Lamentamos informarte de que tu reserva ha sido cancelada debido a labores de mantenimiento programadas en la pista.

        Deporte: %s
        Pista: %s
        Fecha: %s
        Hora: %s

        %s

        Disculpa las molestias. Puedes reservar otra pista u horario desde tu perfil.

        El equipo de Oasis Club.
        """.formatted(userName, sport, courtName, formatReservationDate(date), formatReservationTime(time), refundNote);

    String refundHtml = refunded
        ? """
          <table width="100%%" border="0" cellspacing="0" cellpadding="0">
            <tr>
              <td style="padding: 24px 0 0; text-align: center;">
                <p style="margin: 0; font-family: 'Inter', sans-serif; font-size: 13px; font-weight: 500; color: #0B2118; letter-spacing: 0.02em;">
                  ✓ El importe de tu reserva será reembolsado automáticamente en los próximos 5-10 días hábiles.
                </p>
              </td>
            </tr>
          </table>
          """
        : "";

    String content = buildEmailTitle("Pista en", "Mantenimiento")
        + buildEmailParagraph("""
            Estimado/a <span style="font-weight: 500; color: #111111;">%s</span>,<br><br>
            Lamentamos informarte de que tu reserva ha sido cancelada debido a labores de
            <span style="font-weight: 500; color: #111111;">mantenimiento programadas</span> en la pista.
            """.formatted(escapeHtml(userName)))
        + buildReservationDetailsHtml(sport, courtName, date, time)
        + refundHtml
        + buildEmailFootnote("Disculpa las molestias. Puedes reservar otra pista u horario desde tu perfil.");

    sendEmail(toEmail, subject, plainText, wrapEmailLayout(content), "maintenance-cancellation");
  }

  
  
  

  @Async
  public void sendNewsletterConfirmationEmail(String toEmail) {
    String subject = "Oasis Club | Suscripción a novedades";
    String plainText = """
        OASIS CLUB

        Suscripción confirmada

        Gracias por suscribirte a las novedades de Oasis Club.
        Serás el primero en enterarte de nuevos eventos, torneos y actividades.

        El equipo de Oasis Club.
        """;

    String content = buildEmailTitle("Suscripción", "Confirmada")
        + buildEmailParagraph("""
            Gracias por suscribirte a las novedades de
            <span style="font-weight: 500; color: #111111;">Oasis Club</span>.
            Serás el primero en enterarte de nuevos eventos, torneos y actividades exclusivas
            para socios.
            """)
        + buildEmailFootnote("Puedes cancelar tu suscripción en cualquier momento contactando con nosotros.");

    sendEmail(toEmail, subject, plainText, wrapEmailLayout(content), "newsletter-subscription");
  }

  @Async
  public void sendNewsletterUnsubscribeEmail(String toEmail) {
    String subject = "Oasis Club | Baja del newsletter";
    String plainText = """
        OASIS CLUB

        Baja del newsletter

        Has cancelado tu suscripción a las novedades de Oasis Club.
        Lamentamos verte marchar. Si cambias de opinión, puedes volverte a suscribir cuando quieras.

        El equipo de Oasis Club.
        """;

    String content = buildEmailTitle("Baja", "Confirmada")
        + buildEmailParagraph("""
            Has cancelado tu suscripción al newsletter de
            <span style="font-weight: 500; color: #111111;">Oasis Club</span>.
            Lamentamos verte marchar. Si cambias de opinión, puedes volverte a suscribir
            en cualquier momento desde la sección de Eventos.
            """)
        + buildEmailFootnote("Gracias por haber formado parte de la comunidad.");

    sendEmail(toEmail, subject, plainText, wrapEmailLayout(content), "newsletter-unsubscription");
  }

  
  
  

  @Async
  public void sendEventRegistrationEmail(
      String toEmail,
      String userName,
      String eventTitle,
      LocalDate eventDate,
      LocalTime startTime,
      LocalTime endTime
  ) {
    String subject = "Oasis Club | Inscripción confirmada";
    String plainText = buildEventRegistrationEmailText(userName, eventTitle, eventDate, startTime, endTime);

    String detailsRows = buildDetailRow("Evento", eventTitle)
        + buildDetailRow("Fecha", formatReservationDate(eventDate))
        + buildDetailRow("Horario", formatReservationTime(startTime) + " – " + formatReservationTime(endTime), true);

    String content = buildEmailTitle("Inscripción", "Confirmada")
        + buildEmailParagraph("""
            Estimado/a <span style="font-weight: 500; color: #111111;">%s</span>,<br><br>
            Te confirmamos que tu inscripción al siguiente evento ha sido registrada correctamente.
            """.formatted(escapeHtml(userName)))
        + buildDetailsTable(detailsRows)
        + buildEmailFootnote("Te esperamos en el club.");

    sendEmail(toEmail, subject, plainText, wrapEmailLayout(content), "event-registration");
  }

  @Async
  public void sendEventUnregistrationEmail(
      String toEmail,
      String userName,
      String eventTitle,
      LocalDate eventDate,
      LocalTime startTime,
      LocalTime endTime
  ) {
    String subject = "Oasis Club | Baja de evento confirmada";
    String plainText = """
        OASIS CLUB

        Baja de evento

        Hola, %s.

        Te confirmamos que has cancelado tu inscripción al siguiente evento:

        Evento: %s
        Fecha: %s
        Horario: %s – %s

        Esperamos verte en próximos eventos.
        """.formatted(
            userName,
            eventTitle,
            formatReservationDate(eventDate),
            formatReservationTime(startTime),
            formatReservationTime(endTime)
        );

    String detailsRows = buildDetailRow("Evento", eventTitle)
        + buildDetailRow("Fecha", formatReservationDate(eventDate))
        + buildDetailRow("Horario", formatReservationTime(startTime) + " – " + formatReservationTime(endTime), true);

    String content = buildEmailTitle("Inscripción", "Cancelada")
        + buildEmailParagraph("""
            Estimado/a <span style="font-weight: 500; color: #111111;">%s</span>,<br><br>
            Te confirmamos que has cancelado tu inscripción al siguiente evento.
            """.formatted(escapeHtml(userName)))
        + buildDetailsTable(detailsRows)
        + buildEmailFootnote("Esperamos verte en próximos eventos desde la sección de Agenda.");

    sendEmail(toEmail, subject, plainText, wrapEmailLayout(content), "event-unregistration");
  }

  @Async
  public void sendEventRegistrationRemovedByAdminEmail(
      String toEmail,
      String userName,
      String eventTitle,
      LocalDate eventDate,
      LocalTime startTime,
      LocalTime endTime
  ) {
    String subject = "Oasis Club | Inscripción cancelada";
    String plainText = """
        OASIS CLUB

        Inscripción cancelada

        Hola, %s.

        Te informamos de que tu inscripción al siguiente evento ha sido cancelada por el equipo del club:

        Evento: %s
        Fecha: %s
        Horario: %s – %s

        Si crees que se trata de un error o necesitas más información, ponte en contacto con nosotros.

        El equipo de Oasis Club.
        """.formatted(
            userName,
            eventTitle,
            formatReservationDate(eventDate),
            formatReservationTime(startTime),
            formatReservationTime(endTime)
        );

    String detailsRows = buildDetailRow("Evento", eventTitle)
        + buildDetailRow("Fecha", formatReservationDate(eventDate))
        + buildDetailRow("Horario", formatReservationTime(startTime) + " – " + formatReservationTime(endTime), true);

    String content = buildEmailTitle("Inscripción", "Cancelada")
        + buildEmailParagraph("""
            Estimado/a <span style="font-weight: 500; color: #111111;">%s</span>,<br><br>
            Te informamos de que tu inscripción al siguiente evento ha sido
            <span style="font-weight: 500; color: #111111;">cancelada por el equipo del club</span>.
            """.formatted(escapeHtml(userName)))
        + buildDetailsTable(detailsRows)
        + buildEmailFootnote("Si crees que se trata de un error, ponte en contacto con nosotros.");

    sendEmail(toEmail, subject, plainText, wrapEmailLayout(content), "event-registration-removed-by-admin");
  }

  @Async
  public void sendEventCancelledEmail(
      String toEmail,
      String userName,
      String eventTitle,
      LocalDate eventDate,
      LocalTime startTime,
      LocalTime endTime
  ) {
    String subject = "Oasis Club | Evento cancelado: " + eventTitle;
    String plainText = """
        OASIS CLUB

        Evento cancelado

        Hola, %s.

        Lamentamos informarte de que el siguiente evento al que estabas inscrito/a ha sido cancelado:

        Evento: %s
        Fecha: %s
        Horario: %s – %s

        Disculpa las molestias. Te animamos a consultar otros eventos disponibles en nuestra web.

        El equipo de Oasis Club.
        """.formatted(
            userName,
            eventTitle,
            formatReservationDate(eventDate),
            formatReservationTime(startTime),
            formatReservationTime(endTime)
        );

    String detailsRows = buildDetailRow("Evento", eventTitle)
        + buildDetailRow("Fecha", formatReservationDate(eventDate))
        + buildDetailRow("Horario", formatReservationTime(startTime) + " – " + formatReservationTime(endTime), true);

    String content = buildEmailTitle("Evento", "Cancelado")
        + buildEmailParagraph("""
            Estimado/a <span style="font-weight: 500; color: #111111;">%s</span>,<br><br>
            Lamentamos informarte de que el siguiente evento al que estabas inscrito/a
            ha sido <span style="font-weight: 500; color: #111111;">cancelado</span>.
            """.formatted(escapeHtml(userName)))
        + buildDetailsTable(detailsRows)
        + buildEmailFootnote("Disculpa las molestias. Consulta otros eventos disponibles en la sección de Agenda.");

    sendEmail(toEmail, subject, plainText, wrapEmailLayout(content), "event-cancelled");
  }

  
  
  

  @Async
  public void sendContactFormToClub(
      String nombre,
      String apellidos,
      String email,
      String asunto,
      String mensaje
  ) {
    String subject = "Oasis Club | Nuevo mensaje de contacto: " + asunto;
    String plainText = """
        NUEVO MENSAJE DE CONTACTO

        De: %s %s <%s>
        Asunto: %s

        %s
        """.formatted(nombre, apellidos, email, asunto, mensaje);

    String detailsRows =
        buildDetailRow("Nombre", nombre + " " + apellidos)
        + buildDetailRow("Correo", email)
        + buildDetailRow("Asunto", asunto, true);

    String content = buildEmailTitle("Nuevo", "contacto")
        + buildEmailParagraph("""
            Has recibido un mensaje a través del formulario de contacto de la web.
            Estos son los datos del remitente.
            """)
        + buildDetailsTable(detailsRows)
        + """
          <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="margin-top: 32px;">
            <tr>
              <td>
                <p style="margin: 0 0 12px; font-family: 'Inter', sans-serif; font-size: 11px; font-weight: 600; letter-spacing: 0.15em; text-transform: uppercase; color: #a0a0a0;">
                  Mensaje
                </p>
                <div style="font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 300; color: #111111; line-height: 1.8; white-space: pre-wrap;">%s</div>
              </td>
            </tr>
          </table>
          """.formatted(escapeHtml(mensaje));

    sendEmailDirect("oasisclubmurcia@gmail.com", subject, plainText, wrapEmailLayout(content), "contact-form");
  }

  @Async
  public void sendContactConfirmationToUser(String toEmail, String nombre) {
    String subject = "Oasis Club | Hemos recibido tu mensaje";
    String plainText = """
        OASIS CLUB

        Hemos recibido tu mensaje

        Hola, %s.

        Gracias por ponerte en contacto con Oasis Club. Hemos recibido tu mensaje correctamente y nuestro equipo te responderá en menos de 24 horas.

        El equipo de Oasis Club.
        """.formatted(nombre);

    String content = buildEmailTitle("Mensaje", "Recibido")
        + buildEmailParagraph("""
            Estimado/a <span style="font-weight: 500; color: #111111;">%s</span>,<br><br>
            Gracias por ponerte en contacto con
            <span style="font-weight: 500; color: #111111;">Oasis Club</span>.
            Hemos recibido tu mensaje correctamente y nuestro equipo te responderá en un plazo
            máximo de <span style="font-weight: 500; color: #111111;">24 horas</span>.
            """.formatted(escapeHtml(nombre)))
        + buildEmailFootnote("Si tu consulta es urgente, también puedes llamarnos al +34 968 123 456.");

    sendEmail(toEmail, subject, plainText, wrapEmailLayout(content), "contact-confirmation");
  }

  
  
  

  private void sendEmail(String toEmail, String subject, String plainText, String htmlText, String type) {
    try {
      emailDeliveryService.deliver(toEmail, subject, plainText, htmlText, type, false);
    } catch (Exception e) {
      log.error("Email delivery failed to {} for type {}: {}", toEmail, type, e.getMessage());
    }
  }

  private void sendEmailDirect(String toEmail, String subject, String plainText, String htmlText, String type) {
    try {
      emailDeliveryService.deliver(toEmail, subject, plainText, htmlText, type, true);
    } catch (Exception e) {
      log.error("Direct email delivery failed to {} for type {}: {}", toEmail, type, e.getMessage());
    }
  }

  
  
  

  private String buildEmailTitle(String line1, String line2Italic) {
    return """
        <table width="100%%" border="0" cellspacing="0" cellpadding="0">
          <tr>
            <td style="padding: 0 0 10px;">
              <h1 style="margin: 0; font-family: 'Playfair Display', Georgia, serif; font-size: 42px; font-weight: 400; color: #111111; text-align: center; line-height: 1.15; letter-spacing: -0.02em;">
                %s<br>
                <span style="font-style: italic; color: #0B2118;">%s</span>
              </h1>
            </td>
          </tr>
        </table>
        """.formatted(escapeHtml(line1), escapeHtml(line2Italic));
  }

  private String buildEmailParagraph(String htmlContent) {
    return """
        <table width="100%%" border="0" cellspacing="0" cellpadding="0">
          <tr>
            <td style="padding: 30px 0 36px;">
              <p style="margin: 0; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; font-size: 15px; font-weight: 300; color: #555555; text-align: center; line-height: 1.8;">
                %s
              </p>
            </td>
          </tr>
        </table>
        """.formatted(htmlContent);
  }

  private String buildEmailFootnote(String text) {
    return """
        <table width="100%%" border="0" cellspacing="0" cellpadding="0">
          <tr>
            <td style="padding: 36px 0 0;">
              <p style="margin: 0; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; font-size: 13px; font-weight: 300; color: #999999; text-align: center; line-height: 1.7; letter-spacing: 0.01em;">
                %s
              </p>
            </td>
          </tr>
        </table>
        """.formatted(escapeHtml(text));
  }

  private String buildCtaButton(String url, String label) {
    return """
        <table width="100%%" border="0" cellspacing="0" cellpadding="0">
          <tr>
            <td style="padding: 12px 0 0; text-align: center;">
              <a href="%s" style="display: inline-block; padding: 18px 46px; background-color: #0B2118; color: #ffffff; text-decoration: none; font-family: 'Inter', sans-serif; font-size: 11px; font-weight: 600; letter-spacing: 0.2em; text-transform: uppercase; border-radius: 2px;">
                %s
              </a>
            </td>
          </tr>
        </table>
        """.formatted(url, escapeHtml(label));
  }

  private String buildReservationDetailsHtml(String sport, String courtName, LocalDate date, LocalTime time) {
    String rows = buildDetailRow("Servicio", sport)
        + buildDetailRow("Ubicación", courtName)
        + buildDetailRow("Fecha", formatReservationDate(date))
        + buildDetailRow("Hora", formatReservationTime(time), true);
    return buildDetailsTable(rows);
  }

  private String buildDetailsTable(String rowsHtml) {
    return """
        <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="border-top: 1px solid #f0f0f0; border-bottom: 1px solid #f0f0f0;">
          %s
        </table>
        """.formatted(rowsHtml);
  }

  private String buildDetailRow(String label, String value) {
    return buildDetailRow(label, value, false);
  }

  private String buildDetailRow(String label, String value, boolean isLast) {
    String borderStyle = isLast ? "" : "border-bottom: 1px solid #f8f8f8;";
    return """
        <tr>
          <td style="padding: 22px 0; %s">
            <span style="font-family: 'Inter', sans-serif; font-size: 11px; font-weight: 600; letter-spacing: 0.15em; text-transform: uppercase; color: #a0a0a0;">%s</span>
          </td>
          <td align="right" style="padding: 22px 0; %s">
            <span style="font-family: 'Inter', sans-serif; font-size: 14px; color: #111111; font-weight: 500;">%s</span>
          </td>
        </tr>
        """.formatted(borderStyle, escapeHtml(label), borderStyle, escapeHtml(value));
  }

  
  
  

  private String wrapEmailLayout(String content) {
    int year = Year.now().getValue();
    return """
        <!DOCTYPE html>
        <html lang="es">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Oasis Club</title>
          <style>
            @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&family=Playfair+Display:ital,wght@0,400;0,500;0,600;1,400&display=swap');
            body { margin: 0; padding: 0; background-color: #f7f7f7; font-family: 'Inter', sans-serif; -webkit-font-smoothing: antialiased; }
            table { border-collapse: collapse; }
            @media only screen and (max-width: 620px) {
              .email-outer { padding: 24px 0 !important; }
              .email-card { max-width: 100%% !important; width: 100%% !important; border-left: 0 !important; border-right: 0 !important; border-radius: 0 !important; }
              .email-pad { padding-left: 28px !important; padding-right: 28px !important; }
              .email-pad-top-header { padding: 48px 28px 16px !important; }
              .email-pad-title { padding: 16px 28px 8px !important; }
              .email-pad-body { padding: 24px 28px 36px !important; }
              .email-pad-details { padding: 0 28px 36px !important; }
              .email-pad-cta { padding: 0 28px 48px !important; }
              .email-pad-footer { padding: 32px 28px !important; }
              .email-title h1 { font-size: 32px !important; }
            }
          </style>
        </head>
        <body style="margin: 0; padding: 0; background-color: #f7f7f7; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; -webkit-font-smoothing: antialiased;">
          <table class="email-outer" width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #f7f7f7; padding: 60px 10px;">
            <tr>
              <td align="center">

                <table class="email-card" width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 600px; background-color: #ffffff; border: 1px solid #e5e5e5; border-radius: 4px; box-shadow: 0 15px 35px -15px rgba(0,0,0,0.05);">

                  <!-- Header -->
                  <tr>
                    <td class="email-pad-top-header" style="padding: 70px 50px 20px; text-align: center;">
                      <div style="font-family: 'Playfair Display', Georgia, serif; font-size: 14px; font-weight: 600; letter-spacing: 0.4em; color: #b8975a; text-transform: uppercase;">
                        Oasis Club
                      </div>
                    </td>
                  </tr>

                  <!-- Content -->
                  <tr>
                    <td class="email-pad-body" style="padding: 20px 50px 60px;">
                      %s
                    </td>
                  </tr>

                  <!-- Footer -->
                  <tr>
                    <td class="email-pad-footer" style="padding: 40px 50px; background-color: #fafafa; border-top: 1px solid #f0f0f0; text-align: center;">
                      <p style="margin: 0 0 16px; font-family: 'Inter', sans-serif; font-size: 11px; font-weight: 400; color: #999999; line-height: 1.8; letter-spacing: 0.02em;">
                        ¿Tienes alguna duda? Escríbenos a<br>
                        <a href="mailto:oasisclubmurcia@gmail.com" style="color: #b8975a; text-decoration: none;">oasisclubmurcia@gmail.com</a>
                        &nbsp;·&nbsp;
                        <a href="tel:+34968123456" style="color: #b8975a; text-decoration: none;">+34 968 123 456</a>
                      </p>
                      <p style="margin: 0 0 14px; font-family: 'Inter', sans-serif; font-size: 11px; font-weight: 400; color: #b0b0b0; line-height: 1.7; letter-spacing: 0.02em;">
                        Av. Juan Carlos I, s/n · 30008 Murcia, España<br>
                        Lun – Vie: 07:00 – 23:00 · Sáb – Dom: 08:00 – 22:00
                      </p>
                      <p style="margin: 0; font-family: 'Inter', sans-serif; font-size: 11px; font-weight: 400; color: #999999; letter-spacing: 0.02em;">
                        © %d Oasis Club. Todos los derechos reservados.
                      </p>
                    </td>
                  </tr>

                </table>

              </td>
            </tr>
          </table>
        </body>
        </html>
        """.formatted(content, year);
  }

  
  
  

  private String formatReservationDate(LocalDate date) {
    return date.format(RESERVATION_DATE_FORMATTER);
  }

  private String formatReservationTime(LocalTime time) {
    return time.format(RESERVATION_TIME_FORMATTER);
  }

  private String escapeHtml(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  
  
  

  private String buildResetEmailText(String resetLink) {
    return """
        OASIS CLUB

        Recuperar tu acceso

        Hemos recibido una solicitud para restablecer la contraseña de tu cuenta.
        Usa el siguiente enlace para crear una nueva clave de acceso:
        %s

        Este enlace expirará en 30 minutos.
        Si no solicitaste este cambio, puedes ignorar este correo con total seguridad.
        """.formatted(resetLink);
  }

  private String buildWelcomeEmailText(String userName) {
    return """
        OASIS CLUB

        Bienvenido, %s

        Tu cuenta en Oasis Club ha sido creada y activada correctamente.
        Ya puedes acceder a nuestra plataforma para gestionar tus reservas, explorar nuestras instalaciones y disfrutar de la experiencia premium que ofrecemos.

        Nos vemos en el club.
        """.formatted(userName);
  }

  private String buildReservationConfirmedEmailText(
      String userName,
      String sport,
      String courtName,
      LocalDate date,
      LocalTime time
  ) {
    return """
        OASIS CLUB

        Reserva confirmada

        Hola, %s.

        Tu reserva ha sido confirmada correctamente.

        Servicio: %s
        Ubicación: %s
        Fecha: %s
        Hora: %s

        Te esperamos en el club.
        """.formatted(userName, sport, courtName, formatReservationDate(date), formatReservationTime(time));
  }

  private String buildReservationCancelledEmailText(
      String userName,
      String sport,
      String courtName,
      LocalDate date,
      LocalTime time
  ) {
    return """
        OASIS CLUB

        Reserva cancelada

        Hola, %s.

        Te confirmamos que tu reserva ha sido cancelada correctamente.

        Servicio: %s
        Ubicación: %s
        Fecha: %s
        Hora: %s

        Puedes crear una nueva reserva cuando quieras desde tu perfil.
        """.formatted(userName, sport, courtName, formatReservationDate(date), formatReservationTime(time));
  }

  private String buildEventRegistrationEmailText(
      String userName,
      String eventTitle,
      LocalDate eventDate,
      LocalTime startTime,
      LocalTime endTime
  ) {
    return """
        OASIS CLUB

        Inscripción confirmada

        Hola, %s.

        Te confirmamos que tu inscripción al siguiente evento ha sido registrada correctamente.

        Evento: %s
        Fecha: %s
        Horario: %s – %s

        Te esperamos en el club.
        """.formatted(
            userName,
            eventTitle,
            formatReservationDate(eventDate),
            formatReservationTime(startTime),
            formatReservationTime(endTime)
        );
  }
}
