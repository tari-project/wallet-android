package com.tari.android.wallet.ui.fragment.contact_book.data

import com.tari.android.wallet.R

enum class ContactAction(val icon: Int, val title: Int) {
    Send(R.drawable.vector_contact_action_send, R.string.contact_book_details_send_tari),
    ToFavorite(R.drawable.vector_contact_action_to_favorites, R.string.contact_book_details_add_to_favorites),
    ToUnFavorite(R.drawable.vector_contact_action_to_unfavorites, R.string.contact_book_details_remove_from_favorites),
    OpenProfile(R.drawable.vector_contact_action_details, R.string.contact_book_contacts_book_details_title),
    EditName(R.drawable.tari_empty_drawable, R.string.contact_book_details_edit),
    Link(R.drawable.vector_contact_action_link, R.string.contact_book_contacts_book_link_title),
    Unlink(R.drawable.vector_contact_action_unlink, R.string.contact_book_contacts_book_unlink_title),
    Delete(R.drawable.tari_empty_drawable, R.string.contact_book_details_delete_contact),
}