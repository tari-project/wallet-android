import com.tari.android.wallet.ui.screen.settings.backup.backupOnboarding.item.BackupOnboardingArgs

object BackupOnboardingFlowDataSource {

    private val dataSource = mutableListOf<BackupOnboardingArgs>()

    fun getByPostion(position: Int) : BackupOnboardingArgs = dataSource[position]

    fun save(list: List<BackupOnboardingArgs>) {
        dataSource.clear()
        dataSource.addAll(list)
    }
}