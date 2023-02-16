<script setup>
import avatar1 from '@/assets/images/avatars/avatar-1.png'
import {userManager} from "@/plugins/oidc-settings"
import axios from "axios"

const refInputEl = ref()
const accountDataLocal = ref({})

userManager.signinSilent().then(
  function (user) {
    axios.defaults.headers.common['Authorization'] = `${user.token_type} ${user.access_token}`

    accountDataLocal.value = {
      avatarImg: avatar1,
      firstName: user.profile.given_name,
      lastName: user.profile.family_name,
      email: user.profile.email,
      phone: user.profile.phone_number,
      address: user.profile.address,

    }
  },
)
</script>

<template>
  <VRow>
    <VCol cols="12">
      <VCard title="Account Details">
        <VCardText>
          <!-- 👉 Form -->
          <VForm class="mt-6">
            <VRow>
              <!-- 👉 First Name -->
              <VCol
                md="6"
                cols="12"
              >
                <VTextField
                  v-model="accountDataLocal.firstName"
                  label="First Name"
                  readonly
                />
              </VCol>

              <!-- 👉 Last Name -->
              <VCol
                md="6"
                cols="12"
              >
                <VTextField
                  v-model="accountDataLocal.lastName"
                  label="Last Name"
                  readonly
                />
              </VCol>

              <!-- 👉 Email -->
              <VCol
                cols="12"
                md="6"
              >
                <VTextField
                  v-model="accountDataLocal.email"
                  label="E-mail"
                  type="email"
                  readonly
                />
              </VCol>

              <!-- 👉 Phone -->
              <VCol
                cols="12"
                md="6"
              >
                <VTextField
                  v-model="accountDataLocal.phone"
                  label="Phone Number"
                  readonly
                />
              </VCol>
            </VRow>
          </VForm>
        </VCardText>
      </VCard>
    </VCol>
  </VRow>
</template>
