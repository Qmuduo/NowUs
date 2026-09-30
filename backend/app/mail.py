from email.message import EmailMessage
import smtplib
import ssl

from .config import Settings


class SmtpMailer:
    def __init__(self, settings: Settings):
        self.settings = settings

    def send_otp(self, email: str, code: str) -> None:
        message = EmailMessage()
        message["From"] = self.settings.smtp_from
        message["To"] = email
        message["Subject"] = "NowUs 登录验证码"
        message.set_content(f"你的 NowUs 登录验证码是 {code}，10 分钟内有效。若非本人操作，请忽略此邮件。")
        with smtplib.SMTP(self.settings.smtp_host, self.settings.smtp_port, timeout=10) as smtp:
            smtp.ehlo()
            if self.settings.smtp_starttls:
                smtp.starttls(context=ssl.create_default_context())
                smtp.ehlo()
            if self.settings.smtp_username:
                smtp.login(self.settings.smtp_username, self.settings.smtp_password)
            smtp.send_message(message)
