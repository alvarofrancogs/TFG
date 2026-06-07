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
    String htmlText = buildResetEmailHtml(resetLink);
    sendEmail(toEmail, subject, plainText, htmlText, "password-reset");
  }

  @Async
  public void sendWelcomeEmail(String toEmail, String userName) {
    String subject = "Oasis Club | Bienvenido";
    String plainText = buildWelcomeEmailText(userName);
    String htmlText = buildWelcomeEmailHtml(userName);
    sendEmail(toEmail, subject, plainText, htmlText, "welcome");
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
    String htmlText = buildReservationConfirmedEmailHtml(userName, sport, courtName, date, time);
    sendEmail(toEmail, subject, plainText, htmlText, "reservation-confirmed");
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
    String htmlText = buildReservationCancelledEmailHtml(userName, sport, courtName, date, time);
    sendEmail(toEmail, subject, plainText, htmlText, "reservation-cancelled");
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
          <p style="margin: 20px 0 0; color: #059669; font-size: 14px; font-weight: 500; line-height: 1.6;">
            ✓ El importe de tu reserva será reembolsado automáticamente en los próximos 5-10 días hábiles.
          </p>
          """
        : "";

    String content = """
        <div style="text-align: center;">
          <h2 style="margin: 0 0 16px; font-size: 24px; font-weight: 300; letter-spacing: -0.02em; color: #18181b;">
            Reserva cancelada por mantenimiento
          </h2>
          <p style="margin: 0 0 28px; color: #52525b; font-size: 15px; line-height: 1.7; font-weight: 300;">
            Hola, <strong>%s</strong>. Lamentamos informarte de que tu reserva ha sido cancelada debido a labores de <strong>mantenimiento programadas</strong> en la pista.
          </p>
          %s
          %s
          <div style="margin: 36px 0;">
            <span style="display: inline-block; border: 1px solid #d97706; color: #d97706; padding: 16px 40px; font-size: 13px; font-weight: 600; letter-spacing: 0.1em; text-transform: uppercase;">
              Mantenimiento
            </span>
          </div>
          <p style="margin: 0; color: #a1a1aa; font-size: 13px; line-height: 1.6;">
            Disculpa las molestias. Puedes reservar otra pista u horario desde tu perfil.
          </p>
        </div>
        """.formatted(
            escapeHtml(userName),
            buildReservationDetailsHtml(sport, courtName, date, time),
            refundHtml
        );
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
    String htmlContent = """
        <div style="text-align: center;">
          <h2 style="margin: 0 0 16px; font-size: 24px; font-weight: 300; letter-spacing: -0.02em; color: #18181b;">
            Ya estás suscrito
          </h2>
          <p style="margin: 0 0 20px; color: #52525b; font-size: 15px; line-height: 1.7; font-weight: 300;">
            Gracias por suscribirte a las novedades de <strong>Oasis Club</strong>.
          </p>
          <p style="margin: 0 0 36px; color: #52525b; font-size: 15px; line-height: 1.7; font-weight: 300;">
            Serás el primero en enterarte de nuevos eventos, torneos y actividades exclusivas para socios.
          </p>
          <div style="margin: 36px 0;">
            <span style="display: inline-block; border: 1px solid #022c22; color: #022c22; padding: 16px 40px; font-size: 13px; font-weight: 600; letter-spacing: 0.1em; text-transform: uppercase;">
              Suscripción Activa
            </span>
          </div>
          <p style="margin: 0; color: #a1a1aa; font-size: 13px; line-height: 1.6;">
            Puedes cancelar tu suscripción en cualquier momento contactando con nosotros.
          </p>
        </div>
        """;
    sendEmail(toEmail, subject, plainText, wrapEmailLayout(htmlContent), "newsletter-subscription");
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
    String htmlContent = """
        <div style="text-align: center;">
          <h2 style="margin: 0 0 16px; font-size: 24px; font-weight: 300; letter-spacing: -0.02em; color: #18181b;">
            Baja confirmada
          </h2>
          <p style="margin: 0 0 20px; color: #52525b; font-size: 15px; line-height: 1.7; font-weight: 300;">
            Has cancelado tu suscripción al newsletter de <strong>Oasis Club</strong>.
          </p>
          <p style="margin: 0 0 36px; color: #52525b; font-size: 15px; line-height: 1.7; font-weight: 300;">
            Lamentamos verte marchar. Si cambias de opinión, puedes volverte a suscribir en cualquier momento desde la sección de Eventos.
          </p>
          <div style="margin: 36px 0;">
            <span style="display: inline-block; border: 1px solid #a1a1aa; color: #52525b; padding: 16px 40px; font-size: 13px; font-weight: 600; letter-spacing: 0.1em; text-transform: uppercase;">
              Suscripción Cancelada
            </span>
          </div>
        </div>
        """;
    sendEmail(toEmail, subject, plainText, wrapEmailLayout(htmlContent), "newsletter-unsubscription");
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
    String htmlText = buildEventRegistrationEmailHtml(userName, eventTitle, eventDate, startTime, endTime);
    sendEmail(toEmail, subject, plainText, htmlText, "event-registration");
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

    String content = """
        <div style="text-align: left;">
          <h2 style="margin: 0 0 16px; font-size: 24px; font-weight: 300; letter-spacing: -0.02em; color: #18181b;">
            Nuevo mensaje de contacto
          </h2>
          <div style="background-color: #f9fafb; border: 1px solid #f4f4f5; padding: 24px; margin: 0 0 28px;">
            <table width="100%%" border="0" cellspacing="0" cellpadding="0">
              <tr>
                <td style="padding: 10px 0; color: #71717a; font-size: 12px; font-weight: 600; letter-spacing: 0.12em; text-transform: uppercase; border-bottom: 1px solid #e4e4e7; width: 30%%;">
                  Nombre
                </td>
                <td style="padding: 10px 0; color: #022c22; font-size: 14px; font-weight: 600; border-bottom: 1px solid #e4e4e7;">
                  %s %s
                </td>
              </tr>
              <tr>
                <td style="padding: 10px 0; color: #71717a; font-size: 12px; font-weight: 600; letter-spacing: 0.12em; text-transform: uppercase; border-bottom: 1px solid #e4e4e7;">
                  Correo
                </td>
                <td style="padding: 10px 0; border-bottom: 1px solid #e4e4e7;">
                  <a href="mailto:%s" style="color: #022c22; font-size: 14px; font-weight: 600; text-decoration: underline;">%s</a>
                </td>
              </tr>
              <tr>
                <td style="padding: 10px 0; color: #71717a; font-size: 12px; font-weight: 600; letter-spacing: 0.12em; text-transform: uppercase; border-bottom: 1px solid #e4e4e7;">
                  Asunto
                </td>
                <td style="padding: 10px 0; color: #022c22; font-size: 14px; font-weight: 600; border-bottom: 1px solid #e4e4e7;">
                  %s
                </td>
              </tr>
            </table>
          </div>
          <div style="margin-bottom: 12px;">
            <p style="color: #71717a; font-size: 12px; font-weight: 600; letter-spacing: 0.12em; text-transform: uppercase; margin: 0 0 8px;">Mensaje</p>
            <div style="background-color: #f9fafb; border: 1px solid #f4f4f5; padding: 20px; color: #18181b; font-size: 14px; font-weight: 300; line-height: 1.7; white-space: pre-wrap;">%s</div>
          </div>
        </div>
        """.formatted(
        escapeHtml(nombre), escapeHtml(apellidos),
        escapeHtml(email), escapeHtml(email),
        escapeHtml(asunto),
        escapeHtml(mensaje)
    );
    
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

    String content = """
        <div style="text-align: center;">
          <h2 style="margin: 0 0 16px; font-size: 24px; font-weight: 300; letter-spacing: -0.02em; color: #18181b;">
            Mensaje recibido
          </h2>
          <p style="margin: 0 0 20px; color: #52525b; font-size: 15px; line-height: 1.7; font-weight: 300;">
            Hola, <strong>%s</strong>. Gracias por ponerte en contacto con <strong>Oasis Club</strong>.
          </p>
          <p style="margin: 0 0 36px; color: #52525b; font-size: 15px; line-height: 1.7; font-weight: 300;">
            Hemos recibido tu mensaje correctamente. Nuestro equipo te responderá en un plazo máximo de <strong>24 horas</strong>.
          </p>
          <div style="margin: 36px 0;">
            <span style="display: inline-block; border: 1px solid #022c22; color: #022c22; padding: 16px 40px; font-size: 13px; font-weight: 600; letter-spacing: 0.1em; text-transform: uppercase;">
              Mensaje Enviado
            </span>
          </div>
        </div>
        """.formatted(escapeHtml(nombre));
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

  private String buildResetEmailText(String resetLink) {
    return """
        OASIS CLUB
        
        Recuperar clave

        Hemos recibido una solicitud para restablecer la contraseña de tu cuenta.
        Usa el siguiente enlace para crear una nueva clave de acceso:
        %s

        Este enlace expirara en 30 minutos.
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

        Deporte: %s
        Pista: %s
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

        Deporte: %s
        Pista: %s
        Fecha: %s
        Hora: %s

        Puedes crear una nueva reserva cuando quieras desde tu perfil.
        """.formatted(userName, sport, courtName, formatReservationDate(date), formatReservationTime(time));
  }

  private String buildResetEmailHtml(String resetLink) {
    String content = """
        <div style="text-align: center;">
          <h2 style="margin: 0 0 16px; font-size: 24px; font-weight: 300; letter-spacing: -0.02em; color: #18181b;">
            Recuperar tu clave
          </h2>
          <p style="margin: 0 0 32px; color: #52525b; font-size: 15px; line-height: 1.7; font-weight: 300;">
            Hemos recibido una solicitud para restablecer la contraseña de acceso a tu cuenta. Haz clic en el siguiente enlace para definir una nueva clave de forma segura.
          </p>
          <div style="margin: 36px 0;">
            <a href="%s" style="display: inline-block; background-color: #022c22; color: #ffffff; text-decoration: none; padding: 18px 40px; font-size: 13px; font-weight: 600; letter-spacing: 0.1em; text-transform: uppercase;">
              Restablecer Contraseña
            </a>
          </div>
          <p style="margin: 0 0 12px; color: #71717a; font-size: 13px; line-height: 1.6;">
            <strong>Nota de seguridad:</strong> Este enlace caducará en 30 minutos.
          </p>
          <p style="margin: 0; color: #a1a1aa; font-size: 13px; line-height: 1.6;">
            Si no has solicitado este cambio, por favor ignora este correo. Tu cuenta sigue estando protegida.
          </p>
        </div>
        """.formatted(resetLink);
    return wrapEmailLayout(content);
  }

  private String buildWelcomeEmailHtml(String userName) {
    String content = """
        <div style="text-align: center;">
          <h2 style="margin: 0 0 16px; font-size: 24px; font-weight: 300; letter-spacing: -0.02em; color: #18181b;">
            Nos alegra tenerte, %s
          </h2>
          <p style="margin: 0 0 20px; color: #52525b; font-size: 15px; line-height: 1.7; font-weight: 300;">
            Te confirmamos que tu cuenta en <strong>Oasis Club</strong> ha sido activada correctamente.
          </p>
          <p style="margin: 0 0 36px; color: #52525b; font-size: 15px; line-height: 1.7; font-weight: 300;">
            A partir de este momento puedes acceder a nuestra plataforma exclusiva para gestionar tus reservas, explorar las instalaciones y disfrutar plenamente de la experiencia premium que ofrecemos.
          </p>
          <div style="margin: 36px 0;">
            <span style="display: inline-block; border: 1px solid #022c22; color: #022c22; padding: 16px 40px; font-size: 13px; font-weight: 600; letter-spacing: 0.1em; text-transform: uppercase;">
              Plan Activo
            </span>
          </div>
          <p style="margin: 0; color: #a1a1aa; font-size: 13px; line-height: 1.6;">
            Tu historia en Oasis Club acaba de comenzar. Esperamos verte muy pronto en nuestras instalaciones.
          </p>
        </div>
        """.formatted(userName);
    return wrapEmailLayout(content);
  }

  private String buildReservationConfirmedEmailHtml(
      String userName,
      String sport,
      String courtName,
      LocalDate date,
      LocalTime time
  ) {
    String content = """
        <div style="text-align: center;">
          <h2 style="margin: 0 0 16px; font-size: 24px; font-weight: 300; letter-spacing: -0.02em; color: #18181b;">
            Reserva confirmada
          </h2>
          <p style="margin: 0 0 28px; color: #52525b; font-size: 15px; line-height: 1.7; font-weight: 300;">
            Hola, <strong>%s</strong>. Tu reserva en <strong>Oasis Club</strong> ha sido confirmada correctamente.
          </p>
          %s
          <div style="margin: 36px 0;">
            <span style="display: inline-block; border: 1px solid #022c22; color: #022c22; padding: 16px 40px; font-size: 13px; font-weight: 600; letter-spacing: 0.1em; text-transform: uppercase;">
              Reserva Activa
            </span>
          </div>
          <p style="margin: 0; color: #a1a1aa; font-size: 13px; line-height: 1.6;">
            Te esperamos en el club. Revisa tu perfil si necesitas consultar o gestionar tus reservas.
          </p>
        </div>
        """.formatted(
            escapeHtml(userName),
            buildReservationDetailsHtml(sport, courtName, date, time)
        );
    return wrapEmailLayout(content);
  }

  private String buildReservationCancelledEmailHtml(
      String userName,
      String sport,
      String courtName,
      LocalDate date,
      LocalTime time
  ) {
    String content = """
        <div style="text-align: center;">
          <h2 style="margin: 0 0 16px; font-size: 24px; font-weight: 300; letter-spacing: -0.02em; color: #18181b;">
            Reserva cancelada
          </h2>
          <p style="margin: 0 0 28px; color: #52525b; font-size: 15px; line-height: 1.7; font-weight: 300;">
            Hola, <strong>%s</strong>. Te confirmamos que tu reserva ha sido cancelada correctamente.
          </p>
          %s
          <div style="margin: 36px 0;">
            <span style="display: inline-block; border: 1px solid #a1a1aa; color: #52525b; padding: 16px 40px; font-size: 13px; font-weight: 600; letter-spacing: 0.1em; text-transform: uppercase;">
              Reserva Cancelada
            </span>
          </div>
          <p style="margin: 0; color: #a1a1aa; font-size: 13px; line-height: 1.6;">
            Puedes crear una nueva reserva cuando quieras desde tu perfil.
          </p>
        </div>
        """.formatted(
            escapeHtml(userName),
            buildReservationDetailsHtml(sport, courtName, date, time)
        );
    return wrapEmailLayout(content);
  }

  private String buildReservationDetailsHtml(String sport, String courtName, LocalDate date, LocalTime time) {
    return """
        <div style="background-color: #f9fafb; border: 1px solid #f4f4f5; padding: 24px; text-align: left; margin: 0 0 8px;">
          <table width="100%%" border="0" cellspacing="0" cellpadding="0">
            %s
            %s
            %s
            %s
          </table>
        </div>
        """.formatted(
            buildReservationDetailRow("Deporte", sport),
            buildReservationDetailRow("Pista", courtName),
            buildReservationDetailRow("Fecha", formatReservationDate(date)),
            buildReservationDetailRow("Hora", formatReservationTime(time))
        );
  }

  private String buildReservationDetailRow(String label, String value) {
    return """
        <tr>
          <td style="padding: 10px 0; color: #71717a; font-size: 12px; font-weight: 600; letter-spacing: 0.12em; text-transform: uppercase; border-bottom: 1px solid #e4e4e7;">
            %s
          </td>
          <td align="right" style="padding: 10px 0; color: #022c22; font-size: 14px; font-weight: 600; border-bottom: 1px solid #e4e4e7;">
            %s
          </td>
        </tr>
        """.formatted(escapeHtml(label), escapeHtml(value));
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

  private String wrapEmailLayout(String content) {
    int year = Year.now().getValue();
    return """
        <!DOCTYPE html>
        <html lang="es">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Oasis Club</title>
        </head>
        <body style="margin: 0; padding: 0; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f4f4f5; -webkit-font-smoothing: antialiased;">
          <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #f4f4f5; padding: 40px 20px;">
            <tr>
              <td align="center">
                
                <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 600px; background-color: #ffffff; border-top: 4px solid #022c22; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.01);">
                  
                  <tr>
                    <td style="padding: 48px 48px 0 48px; text-align: center;">
                      <div style="font-size: 20px; font-weight: 700; color: #022c22; letter-spacing: 0.3em; text-transform: uppercase; margin-bottom: 32px;">
                        OASIS CLUB
                      </div>
                      <div style="height: 1px; background-color: #e4e4e7; width: 100%%;"></div>
                    </td>
                  </tr>
        
                  <tr>
                    <td style="padding: 40px 48px;">
                      %s
                    </td>
                  </tr>
        
                  <tr>
                    <td style="padding: 0 48px 40px 48px;">
                      <div style="background-color: #f9fafb; border: 1px solid #f4f4f5; padding: 24px; text-align: center;">
                        <p style="margin: 0; color: #52525b; font-size: 13px; font-weight: 400; line-height: 1.6;">
                          ¿Tienes alguna duda o necesitas ayuda?<br>
                          Contacta con nuestro equipo de soporte en<br>
                          <a href="mailto:oasisclubmurcia@gmail.com" style="color: #022c22; text-decoration: underline; font-weight: 600;">oasisclubmurcia@gmail.com</a>
                        </p>
                      </div>
                    </td>
                  </tr>
        
                  <tr>
                    <td style="padding: 32px 48px; background-color: #022c22; text-align: center;">
                      <p style="margin: 0 0 12px; color: #d4d4d8; font-size: 11px; letter-spacing: 0.15em; text-transform: uppercase; font-weight: 600;">
                        © %d OASIS CLUB. RESERVADOS TODOS LOS DERECHOS.
                      </p>
                      <p style="margin: 0; color: #a1a1aa; font-size: 11px; line-height: 1.6;">
                        Urb. Oasis de los Alcázares · Región de Murcia, España<br>
                        Este es un mensaje automático, por favor no respondas directamente a este correo.
                      </p>
                    </td>
                  </tr>
        
                </table>
        
                <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 600px;">
                  <tr>
                    <td style="padding: 24px 0; text-align: center;">
                      <p style="margin: 0; color: #71717a; font-size: 12px;">
                        Protegemos la privacidad y seguridad de tus datos
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

  private String buildEventRegistrationEmailHtml(
      String userName,
      String eventTitle,
      LocalDate eventDate,
      LocalTime startTime,
      LocalTime endTime
  ) {
    String content = """
        <div style="text-align: center;">
          <h2 style="margin: 0 0 16px; font-size: 24px; font-weight: 300; letter-spacing: -0.02em; color: #18181b;">
            Inscripción confirmada
          </h2>
          <p style="margin: 0 0 28px; color: #52525b; font-size: 15px; line-height: 1.7; font-weight: 300;">
            Hola, <strong>%s</strong>. Tu inscripción en <strong>Oasis Club</strong> ha sido registrada correctamente.
          </p>
          <div style="background-color: #f9fafb; border: 1px solid #f4f4f5; padding: 24px; text-align: left; margin: 0 0 28px;">
            <table width="100%%" border="0" cellspacing="0" cellpadding="0">
              %s
              %s
              %s
            </table>
          </div>
          <div style="margin: 36px 0;">
            <span style="display: inline-block; border: 1px solid #022c22; color: #022c22; padding: 16px 40px; font-size: 13px; font-weight: 600; letter-spacing: 0.1em; text-transform: uppercase;">
              Inscripción Activa
            </span>
          </div>
          <p style="margin: 0; color: #a1a1aa; font-size: 13px; line-height: 1.6;">
            Te esperamos en el club. Puedes consultar tus eventos desde tu perfil.
          </p>
        </div>
        """.formatted(
        escapeHtml(userName),
        buildReservationDetailRow("Evento", eventTitle),
        buildReservationDetailRow("Fecha", formatReservationDate(eventDate)),
        buildReservationDetailRow("Horario", formatReservationTime(startTime) + " – " + formatReservationTime(endTime))
    );
    return wrapEmailLayout(content);
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

    String content = """
        <div style="text-align: center;">
          <h2 style="margin: 0 0 16px; font-size: 24px; font-weight: 300; letter-spacing: -0.02em; color: #18181b;">
            Baja de evento confirmada
          </h2>
          <p style="margin: 0 0 28px; color: #52525b; font-size: 15px; line-height: 1.7; font-weight: 300;">
            Hola, <strong>%s</strong>. Te confirmamos que has cancelado tu inscripción al evento.
          </p>
          <div style="background-color: #f9fafb; border: 1px solid #f4f4f5; padding: 24px; text-align: left; margin: 0 0 28px;">
            <table width="100%%" border="0" cellspacing="0" cellpadding="0">
              %s
              %s
              %s
            </table>
          </div>
          <div style="margin: 36px 0;">
            <span style="display: inline-block; border: 1px solid #a1a1aa; color: #52525b; padding: 16px 40px; font-size: 13px; font-weight: 600; letter-spacing: 0.1em; text-transform: uppercase;">
              Inscripción Cancelada
            </span>
          </div>
          <p style="margin: 0; color: #a1a1aa; font-size: 13px; line-height: 1.6;">
            Puedes consultar próximos eventos desde la sección de Agenda.
          </p>
        </div>
        """.formatted(
        escapeHtml(userName),
        buildReservationDetailRow("Evento", eventTitle),
        buildReservationDetailRow("Fecha", formatReservationDate(eventDate)),
        buildReservationDetailRow("Horario", formatReservationTime(startTime) + " – " + formatReservationTime(endTime))
    );
    sendEmail(toEmail, subject, plainText, wrapEmailLayout(content), "event-unregistration");
  }
}

