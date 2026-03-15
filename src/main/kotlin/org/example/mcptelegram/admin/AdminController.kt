package org.example.mcptelegram.admin

import org.example.mcptelegram.persistence.repository.DialogRepository
import org.example.mcptelegram.persistence.repository.MessageRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/admin")
class AdminController(
    private val dialogRepository: DialogRepository,
    private val messageRepository: MessageRepository
) {

    @GetMapping
    fun dashboard(model: Model): String {
        model.addAttribute("dialogCount", dialogRepository.count())
        model.addAttribute("messageCount", messageRepository.count())
        return "admin/dashboard"
    }

    @GetMapping("/dialogs")
    fun dialogs(model: Model): String {
        model.addAttribute("dialogs", dialogRepository.findAllByOrderByLastMessageAtDesc())
        return "admin/dialogs"
    }

    @GetMapping("/dialogs/{id}")
    fun dialogDetail(@PathVariable id: Long, model: Model): String {
        val dialog = dialogRepository.findById(id).orElseThrow { NoSuchElementException("Dialog not found") }
        val messages = messageRepository.findByDialogOrderBySentAtDesc(
            dialog,
            PageRequest.of(0, 50)
        )
        model.addAttribute("dialog", dialog)
        model.addAttribute("messages", messages)
        return "admin/dialog-detail"
    }

    @GetMapping("/log")
    fun log(): String = "admin/log"
}
