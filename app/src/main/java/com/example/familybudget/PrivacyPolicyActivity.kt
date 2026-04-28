package com.example.familybudget

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PrivacyPolicyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_policy)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val tvPolicyText = findViewById<TextView>(R.id.tv_policy_text)
        tvPolicyText.text = getPrivacyPolicyText()
    }

    private fun getPrivacyPolicyText(): String {
        return """
            Политика конфиденциальности приложения «Финансовый Компас»

            Дата вступления в силу: 27 апреля 2026 г.

            1. Общие положения

            Добро пожаловать в «Финансовый Компас» (далее — «Приложение»). 
            Мы уважаем вашу конфиденциальность и обязуемся защищать ваши личные данные.

            2. Какие данные мы собираем

            • Все ваши финансовые данные (доходы, расходы, категории, лимиты, списки покупок, мечты) 
              хранятся исключительно на вашем устройстве в локальной базе данных.
            • Мы НЕ собираем и НЕ передаём эти данные на наши серверы.

            3. Как мы используем данные

            Ваши данные никогда не покидают ваше устройство и используются только для:
            • Ведения учёта ваших финансов
            • Отображения статистики и графиков
            • Формирования списков покупок

            4. Передача данных третьим лицам

            Мы не продаём, не обмениваем и не передаём ваши личные данные третьим лицам.

            Приложение использует библиотеки:
            • Room — для локального хранения данных
            • MPAndroidChart — для графиков
            • Material Design — для интерфейса

            5. Разрешения

            • Уведомления — для оповещений о превышении лимита
            • Интернет — только для отправки обратной связи (через Google Forms)

            6. Безопасность

            Все ваши финансовые данные хранятся в зашифрованной базе данных на вашем устройстве.
            Мы не имеем к ним удалённого доступа.

            7. Ваши права

            Вы имеете полный контроль над своими данными:
            • Просматривать, редактировать и удалять их через интерфейс приложения
            • Полное удаление данных происходит при очистке данных приложения или его удалении

            8. Обратная связь

            Если у вас есть вопросы, воспользуйтесь формой обратной связи в приложении 
            (меню → Разработчикам).

            9. Изменения в Политике

            Мы можем обновлять эту Политику. О существенных изменениях мы уведомим через обновление приложения.

            © 2026 Uncle_Griff_studio
        """.trimIndent()
    }
}