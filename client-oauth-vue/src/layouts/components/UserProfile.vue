<script>
import avatar1 from '@/assets/images/avatars/avatar-1.png'
import {cryptoUser} from "@/plugins/crypto-user-utils";
import {store} from "@/plugins/store";

let accountDataLocal = ref('')

let user = await cryptoUser.getUserInfo()
let isLogged = (user !== undefined)

if (isLogged) {
  accountDataLocal.value = {
    avatarImg: avatar1,
    name: user.name,
    firstName: user.given_name,
    lastName: user.family_name,
    email: user.email,
    phone: user.phone_number,
    address: user.address
  }
}


const avatarBadgeProps = {
  dot: true,
  location: 'bottom right',
  offsetX: 3,
  offsetY: 3,
  color: 'success',
  bordered: true,
}

export default {
  data() {
    return {
      store,
      accountDataLocal,
      avatar1,
      avatarBadgeProps
    }
  }
}


</script>

<template>
  <VBadge v-bind="avatarBadgeProps">
    <VLabel>
      {{ store.getters.inUseWallet.balance + " ETH" }}
    </VLabel>
    <VAvatar
      style="cursor: pointer;"
      color="primary"
      variant="tonal"
    >
      <VImg :src="avatar1"/>

      <!-- SECTION Menu -->
      <VMenu
        activator="parent"
        width="230"
        location="bottom end"
        offset="14px"
      >
        <VList>
          <!-- 👉 User Avatar & Name -->
          <VListItem>
            <template #prepend>
              <VListItemAction>
                <VBadge v-bind="avatarBadgeProps">
                  <VAvatar
                    color="primary"
                    size="40"
                    variant="tonal"
                  >
                    <VImg :src="avatar1"/>
                  </VAvatar>
                </VBadge>
              </VListItemAction>
            </template>

            <VListItemTitle class="font-weight-semibold">
              {{ accountDataLocal.name }}
            </VListItemTitle>
            <VListItemSubtitle class="text-disabled">
              Admin
            </VListItemSubtitle>
          </VListItem>

          <VDivider class="my-2"/>

          <!-- 👉 Profile -->
          <VListItem link>
            <template #prepend>
              <VIcon
                class="me-2"
                icon="mdi-account-outline"
                size="22"
              />
            </template>

            <VListItemTitle>Profile</VListItemTitle>
          </VListItem>

          <!-- 👉 Settings -->
          <VListItem link>
            <template #prepend>
              <VIcon
                class="me-2"
                icon="mdi-cog-outline"
                size="22"
              />
            </template>

            <VListItemTitle>Settings</VListItemTitle>
          </VListItem>

          <!-- Divider -->
          <VDivider class="my-2"/>

          <!-- 👉 Logout -->
          <VListItem to="/logout">
            <template #prepend>
              <VIcon
                class="me-2"
                icon="mdi-logout-variant"
                size="22"
              />
            </template>

            <VListItemTitle>Logout</VListItemTitle>
          </VListItem>
        </VList>
      </VMenu>
      <!-- !SECTION -->
    </VAvatar>
  </VBadge>
</template>
